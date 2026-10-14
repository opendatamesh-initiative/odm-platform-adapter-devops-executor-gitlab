package org.opendatamesh.platform.up.executor.gitlab.client.gitlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources.GitLabCreatePipelineReq;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources.GitLabGraphQlReq;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources.GitLabJobRes;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources.GitLabPipelineRes;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources.GitLabProjectRes;
import org.opendatamesh.platform.up.executor.gitlab.config.GitLabExecutorProperties;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ExecutorGitlabApiException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.InternalException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.NotFoundException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ResourceConflictException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.client.ClientResourceMappingException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.DeclaredInput;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabJob;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputSpecLookup;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputType;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.SelectedInput;
import org.opendatamesh.platform.up.executor.gitlab.utils.client.RestUtils;
import org.opendatamesh.platform.up.executor.gitlab.utils.client.RestUtilsFactory;
import org.opendatamesh.platform.up.executor.gitlab.utils.client.http.HttpHeader;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@ApplicationScoped
public class GitLabClientImpl implements GitLabClient {

    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CACHE_MISS_MESSAGE = "Failed to retrieve pipeline inputs from cache";
    private static final String UNREADABLE_RESPONSE = "Unreadable GitLab response";
    private static final String INPUTS_QUERY = "query($p: ID!, $r: String!) { project(fullPath: $p) { "
            + "ciPipelineCreationInputs(ref: $r, failOnCacheMiss: true) { name type required } } }";
    private static final int HTTP_NOT_FOUND = 404;
    private static final int JOBS_PER_PAGE = 100;
    private static final int MAX_JOB_PAGES = 50;
    private static final long RETRY_PAUSE_MILLIS = 500;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_ERROR_BODY_CHARS = 500;
    private static final int MAX_NOT_FOUND_BODY_CHARS = 200;

    private final GitLabExecutorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RestUtils rest;

    public GitLabClientImpl(GitLabExecutorProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.http().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.rest = RestUtilsFactory.getRestUtils(httpClient, objectMapper);
    }

    @Override
    public String findProjectPath(GitLabAccess access) {
        return readProject(access).pathWithNamespace();
    }

    @Override
    public InputSpecLookup findDeclaredInputs(GitLabAccess access, String projectPath, String ref) {
        JsonNode response = readInputSpec(access, projectPath, ref);
        return toInputSpec(response, access.projectId(), ref);
    }

    @Override
    public String createPipeline(GitLabAccess access, String ref, List<SelectedInput> inputs) {
        return String.valueOf(postNewPipeline(access, ref, inputs).id());
    }

    @Override
    public PipelineState getPipeline(GitLabAccess access, String pipelineId) {
        GitLabPipelineRes pipeline = readPipeline(access, pipelineId);
        return new PipelineState(String.valueOf(pipeline.id()), pipeline.status());
    }

    @Override
    public void cancelPipeline(GitLabAccess access, String pipelineId) {
        postCancel(access, pipelineId);
    }

    @Override
    public List<GitLabJob> listJobs(GitLabAccess access, String pipelineId) {
        return toSortedJobs(readAllJobPages(access, pipelineId));
    }

    @Override
    public Optional<String> readJobLog(GitLabAccess access, long jobId) {
        return withReadRetries(() -> fetchJobLog(access, jobId));
    }

    private GitLabProjectRes readProject(GitLabAccess access) {
        return requireReadable(withReadRetries(() ->
                rest.genericGet(projectUrl(access), headers(access, APPLICATION_JSON), null, GitLabProjectRes.class)));
    }

    private JsonNode readInputSpec(GitLabAccess access, String projectPath, String ref) {
        GitLabGraphQlReq request = new GitLabGraphQlReq(INPUTS_QUERY, Map.of("p", projectPath, "r", ref));
        return requireReadable(withReadRetries(() ->
                rest.genericPost(graphQlUrl(access), headers(access, APPLICATION_JSON), request, JsonNode.class)));
    }

    private GitLabPipelineRes postNewPipeline(GitLabAccess access, String ref, List<SelectedInput> inputs) {
        GitLabCreatePipelineReq request = new GitLabCreatePipelineReq(ref, toTypedInputs(inputs));
        return requireReadable(sendOnce(() ->
                rest.genericPost(pipelinesUrl(access), headers(access, APPLICATION_JSON), request, GitLabPipelineRes.class)));
    }

    private GitLabPipelineRes readPipeline(GitLabAccess access, String pipelineId) {
        return requireReadable(withReadRetries(() ->
                rest.genericGet(pipelineUrl(access, pipelineId), headers(access, APPLICATION_JSON), null, GitLabPipelineRes.class)));
    }

    private void postCancel(GitLabAccess access, String pipelineId) {
        sendOnce(() -> rest.genericPost(
                pipelineUrl(access, pipelineId) + "/cancel", headers(access, APPLICATION_JSON), null, JsonNode.class));
    }

    private List<GitLabJobRes> readAllJobPages(GitLabAccess access, String pipelineId) {
        List<GitLabJobRes> all = new ArrayList<>();
        for (int page = 1; page <= MAX_JOB_PAGES; page++) {
            List<GitLabJobRes> items = readJobPage(access, pipelineId, page);
            all.addAll(items);
            if (items.size() < JOBS_PER_PAGE) {
                break;
            }
        }
        return all;
    }

    private List<GitLabJobRes> readJobPage(GitLabAccess access, String pipelineId, int page) {
        String url = pipelineUrl(access, pipelineId) + "/jobs?per_page=" + JOBS_PER_PAGE + "&page=" + page;
        GitLabJobRes[] items = requireReadable(withReadRetries(() ->
                rest.genericGet(url, headers(access, APPLICATION_JSON), null, GitLabJobRes[].class)));
        return Arrays.asList(items);
    }

    private Optional<String> fetchJobLog(GitLabAccess access, long jobId) {
        HttpResponse<byte[]> response = getText(access, projectUrl(access) + "/jobs/" + jobId + "/trace");
        if (response.statusCode() == HTTP_NOT_FOUND) {
            return Optional.empty();
        }
        if (response.statusCode() >= 300) {
            throw new ClientException(response.statusCode(), bodyText(response));
        }
        return Optional.of(bodyText(response));
    }

    private HttpResponse<byte[]> getText(GitLabAccess access, String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header(PRIVATE_TOKEN, access.token())
                .header(ACCEPT, TEXT_PLAIN)
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new ClientException(500, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClientException(500, e.getMessage());
        }
    }

    private InputSpecLookup toInputSpec(JsonNode response, String projectId, String ref) {
        JsonNode errors = response.path("errors");
        if (containsCacheMiss(errors)) {
            return InputSpecLookup.notReady();
        }
        JsonNode project = response.at("/data/project");
        if (errors.size() > 0 || isAbsent(project)) {
            throw projectNotFound(projectId, ref);
        }
        JsonNode inputs = project.path("ciPipelineCreationInputs");
        if (isAbsent(inputs)) {
            return InputSpecLookup.notReady();
        }
        return InputSpecLookup.ready(toDeclaredInputs(inputs));
    }

    private Map<String, Object> toTypedInputs(List<SelectedInput> inputs) {
        Map<String, Object> typed = new LinkedHashMap<>();
        for (SelectedInput selected : inputs) {
            typed.put(selected.input().name(), toInputValue(selected));
        }
        return typed;
    }

    private Object toInputValue(SelectedInput selected) {
        String raw = selected.rawValue();
        if (raw == null) {
            return null;
        }
        try {
            return switch (selected.input().type()) {
                case NUMBER -> new BigDecimal(raw.trim());
                case BOOLEAN -> parseBoolean(raw);
                case ARRAY -> objectMapper.readValue(raw, List.class);
                case STRING -> raw;
            };
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return raw;
        }
    }

    private static Object parseBoolean(String raw) {
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.valueOf(raw);
        }
        return raw;
    }

    private List<GitLabJob> toSortedJobs(List<GitLabJobRes> items) {
        return items.stream()
                .sorted(Comparator.comparingLong(GitLabJobRes::id))
                .map(GitLabClientImpl::toJob)
                .toList();
    }

    private static GitLabJob toJob(GitLabJobRes res) {
        return new GitLabJob(res.id(), res.name(), res.stage(), res.status());
    }

    private static List<DeclaredInput> toDeclaredInputs(JsonNode inputs) {
        List<DeclaredInput> declared = new ArrayList<>();
        for (JsonNode input : inputs) {
            declared.add(new DeclaredInput(
                    input.path("name").asText(null),
                    InputType.fromGitLab(input.path("type").asText(null)),
                    input.path("required").asBoolean(false)));
        }
        return declared;
    }

    private static boolean containsCacheMiss(JsonNode errors) {
        for (JsonNode error : errors) {
            if (error.path("message").asText("").contains(CACHE_MISS_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAbsent(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }

    private static NotFoundException projectNotFound(String projectId, String ref) {
        return new NotFoundException("GitLab project " + projectId + " or ref " + ref + " not found or not accessible");
    }

    private <T> T withReadRetries(Supplier<T> call) {
        return callGitLab(call, properties.http().readRetries());
    }

    private <T> T sendOnce(Supplier<T> call) {
        return callGitLab(call, 0);
    }

    private <T> T callGitLab(Supplier<T> call, int retries) {
        int attempt = 0;
        while (true) {
            try {
                return call.get();
            } catch (ClientException e) {
                if (attempt >= retries || !isRetryable(e)) {
                    throw translate(e);
                }
                attempt++;
                pauseBeforeRetry();
            } catch (ClientResourceMappingException e) {
                throw new BadGatewayException(UNREADABLE_RESPONSE, e);
            }
        }
    }

    private static boolean isRetryable(ClientException e) {
        return e.getCode() == 429 || e.getCode() >= 500;
    }

    private static void pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_PAUSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("GitLab call interrupted", e);
        }
    }

    private static ExecutorGitlabApiException translate(ClientException e) {
        int code = e.getCode();
        return switch (code) {
            case 400, 422 -> new BadRequestException(
                    "GitLab rejected the request: " + truncate(e.getResponseBody(), MAX_ERROR_BODY_CHARS));
            case 404 -> new NotFoundException(
                    "GitLab resource not found: " + truncate(e.getResponseBody(), MAX_NOT_FOUND_BODY_CHARS));
            case 409 -> new ResourceConflictException(
                    "GitLab reported a conflict: " + truncate(e.getResponseBody(), MAX_ERROR_BODY_CHARS));
            case 401, 403 -> new BadGatewayException("GitLab refused the access token (HTTP " + code + ")");
            default -> new BadGatewayException("GitLab call failed (HTTP " + code + ")");
        };
    }

    private static <T> T requireReadable(T value) {
        if (value == null) {
            throw new BadGatewayException(UNREADABLE_RESPONSE);
        }
        return value;
    }

    private static List<HttpHeader> headers(GitLabAccess access, String accept) {
        return List.of(new HttpHeader(PRIVATE_TOKEN, access.token()), new HttpHeader(ACCEPT, accept));
    }

    private static String projectUrl(GitLabAccess access) {
        return access.instance().baseUrl() + "/api/v4/projects/" + encode(access.projectId());
    }

    private static String pipelinesUrl(GitLabAccess access) {
        return projectUrl(access) + "/pipeline";
    }

    private static String pipelineUrl(GitLabAccess access, String pipelineId) {
        return projectUrl(access) + "/pipelines/" + encode(pipelineId);
    }

    private static String graphQlUrl(GitLabAccess access) {
        return access.instance().baseUrl() + "/api/graphql";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String bodyText(HttpResponse<byte[]> response) {
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
