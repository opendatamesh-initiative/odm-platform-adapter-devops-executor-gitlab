package org.opendatamesh.platform.up.executor.gitlab.testsupport;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Request builders for the four executor endpoints, with the bodies the DevOps server sends.
 */
public final class ExecutorApi {

    public static final String TOKEN_HEADER = "x-odm-token";
    public static final String DEFAULT_TOKEN = "t0k";
    public static final String PROJECT_ID = "123";
    public static final String REF = "v1.2.0";

    private static final String BASE_PATH = "/api/v2/up/executor/tasks/";

    private ExecutorApi() {
    }

    /** The stub's base URL, as the DevOps server sends it in {@code providerBaseUrl}. */
    public static String stubUrl() {
        return GitLabStubResource.stub().baseUrl();
    }

    /** Executor parameters for project {@value #PROJECT_ID} and ref {@value #REF} on the stub. */
    public static Map<String, Object> executorParameters() {
        return executorParameters(stubUrl(), PROJECT_ID, REF);
    }

    public static Map<String, Object> executorParameters(String providerBaseUrl, String externalIdentifier, String ref) {
        Map<String, Object> repo = new LinkedHashMap<>();
        repo.put("providerType", "GITLAB");
        repo.put("providerBaseUrl", providerBaseUrl);
        repo.put("externalIdentifier", externalIdentifier);

        Map<String, Object> gitRef = new LinkedHashMap<>();
        gitRef.put("name", ref);
        gitRef.put("type", "TAG");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("dataProductRepo", repo);
        parameters.put("ref", gitRef);
        return parameters;
    }

    /** Body for {@code /start}: executor parameters plus pipeline parameters. */
    public static Map<String, Object> startBody(Map<String, String> pipelineParameters) {
        return startBody(executorParameters(), pipelineParameters);
    }

    public static Map<String, Object> startBody(Map<String, Object> executorParameters,
                                                Map<String, String> pipelineParameters) {
        Map<String, Object> body = new HashMap<>();
        body.put("executorParameters", executorParameters);
        body.put("pipelineParameters", pipelineParameters);
        return body;
    }

    /** Body for {@code /status}, {@code /logs} and {@code /cancel}: executor parameters plus the run id. */
    public static Map<String, Object> runBody(String providerRunId) {
        return runBody(executorParameters(), providerRunId);
    }

    public static Map<String, Object> runBody(Map<String, Object> executorParameters, String providerRunId) {
        Map<String, Object> body = new HashMap<>();
        body.put("executorParameters", executorParameters);
        body.put("providerRunId", providerRunId);
        return body;
    }

    /**
     * POSTs {@code body} to {@code /api/v2/up/executor/tasks/{endpoint}}. A {@code null} token sends no
     * {@code x-odm-token} header.
     */
    public static Response post(String endpoint, Object body, String token) {
        var request = given().contentType(ContentType.JSON).body(body);
        if (token != null) {
            request.header(TOKEN_HEADER, token);
        }
        return request.when().post(BASE_PATH + endpoint);
    }
}
