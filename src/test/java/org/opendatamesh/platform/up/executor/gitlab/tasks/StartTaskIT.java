package org.opendatamesh.platform.up.executor.gitlab.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub.DeclaredInputSpec;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStubResource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.DEFAULT_TOKEN;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.post;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.startBody;

@QuarkusTest
@QuarkusTestResource(GitLabStubResource.class)
class StartTaskIT {

    private static final String CREATE_PIPELINE = "/api/v4/projects/123/pipeline";
    private static final String GRAPHQL = "/api/graphql";
    private static final ObjectMapper JSON = new ObjectMapper();

    private GitLabStub stub;

    @BeforeEach
    void resetStub() {
        stub = GitLabStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: Start selects declared inputs and returns the pipeline id
     *   Given the CI configuration at ref "v1.2.0" declares inputs "env" (string, required) and "replicas" (number, optional)
     *   And the pipeline parameters are env=prod, replicas=3 and extra=x
     *   When DevOps starts the task with token "t0k"
     *   Then GitLab receives a pipeline for ref "v1.2.0" with inputs env="prod" and replicas=3 as a number
     *   And the input "extra" is not sent
     *   And the response providerRunId is the GitLab pipeline id
     *   And every GitLab call carried PRIVATE-TOKEN "t0k"
     */
    @Test
    void whenStartingWithParametersThenOnlyDeclaredInputsAreSentTyped() throws Exception {
        stub.declareInputs(
                new DeclaredInputSpec("env", "STRING", true),
                new DeclaredInputSpec("replicas", "NUMBER", false));

        Response response = post("start",
                startBody(Map.of("env", "prod", "replicas", "3", "extra", "x")), DEFAULT_TOKEN);

        response.then().statusCode(200).body("providerRunId", equalTo("42"));
        JsonNode create = JSON.readTree(stub.requestsTo("POST", CREATE_PIPELINE).get(0).body());
        assertEquals("v1.2.0", create.path("ref").asText());
        assertEquals("prod", create.path("inputs").path("env").asText());
        JsonNode replicas = create.path("inputs").path("replicas");
        assertTrue(replicas.isNumber());
        assertEquals(0, new BigDecimal("3").compareTo(replicas.decimalValue()));
        assertFalse(create.path("inputs").has("extra"));

        JsonNode inputsQuery = JSON.readTree(stub.requests("POST", GRAPHQL).get(0).body());
        assertEquals("group/project", inputsQuery.path("variables").path("p").asText());
        assertEquals("v1.2.0", inputsQuery.path("variables").path("r").asText());

        assertFalse(stub.requests().isEmpty());
        assertTrue(stub.requests().stream().allMatch(r -> DEFAULT_TOKEN.equals(r.privateToken())));
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: A required input without a parameter is refused
     *   Given the CI configuration declares the required input "env"
     *   And the pipeline parameters do not contain "env"
     *   When DevOps starts the task
     *   Then the response is 400 naming "env"
     *   And no pipeline is created in GitLab
     */
    @Test
    void whenRequiredInputMissingThen400AndNoPipeline() {
        stub.declareInputs(new DeclaredInputSpec("env", "STRING", true));

        post("start", startBody(Map.of("replicas", "3")), DEFAULT_TOKEN)
                .then().statusCode(400)
                .body(containsString("env"));

        assertTrue(stub.requestsTo("POST", CREATE_PIPELINE).isEmpty());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: A value that cannot be converted is sent as text
     *   Given the CI configuration declares the input "replicas" of type number
     *   And the pipeline parameter replicas is "three"
     *   When DevOps starts the task
     *   Then GitLab receives replicas="three" as a string
     */
    @Test
    void whenNumberInputIsNotNumericThenSentAsString() throws Exception {
        stub.declareInputs(new DeclaredInputSpec("replicas", "NUMBER", true));

        post("start", startBody(Map.of("replicas", "three")), DEFAULT_TOKEN)
                .then().statusCode(200);

        JsonNode replicas = JSON.readTree(stub.requestsTo("POST", CREATE_PIPELINE).get(0).body())
                .path("inputs").path("replicas");
        assertTrue(replicas.isTextual());
        assertEquals("three", replicas.asText());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: A cold input cache is retried and then succeeds
     *   Given GitLab answers the input query with "Failed to retrieve pipeline inputs from cache" twice, then with the declared inputs
     *   When DevOps starts the task
     *   Then the pipeline is created
     */
    @Test
    void whenInputCacheColdTwiceThenPipelineCreated() {
        stub.declareInputs(new DeclaredInputSpec("env", "STRING", true));
        stub.coldInputCacheTimes(2);

        post("start", startBody(Map.of("env", "prod")), DEFAULT_TOKEN)
                .then().statusCode(200).body("providerRunId", equalTo("42"));

        assertEquals(3, stub.requests("POST", GRAPHQL).size());
        assertEquals(1, stub.requestsTo("POST", CREATE_PIPELINE).size());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: An input cache that stays cold fails the start
     *   Given GitLab always answers the input query with "Failed to retrieve pipeline inputs from cache"
     *   When DevOps starts the task
     *   Then the response is 503
     *   And no pipeline is created in GitLab
     */
    @Test
    void whenInputCacheStaysColdThen503AndNoPipeline() {
        stub.declareInputs(new DeclaredInputSpec("env", "STRING", true));
        stub.coldInputCacheAlways();

        post("start", startBody(Map.of("env", "prod")), DEFAULT_TOKEN)
                .then().statusCode(503);

        // cache-miss-budget 300ms / cache-miss-retry-interval 50ms = 6 reads
        assertEquals(6, stub.requests("POST", GRAPHQL).size());
        assertTrue(stub.requestsTo("POST", CREATE_PIPELINE).isEmpty());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: A GitLab instance outside the allow-list is refused
     *   Given executorParameters.dataProductRepo.providerBaseUrl is "https://evil.example.com"
     *   When DevOps starts the task
     *   Then the response is 400
     *   And no request reaches any GitLab
     */
    @Test
    void whenInstanceNotAllowedThen400AndNoGitLabCall() {
        Map<String, Object> body = startBody(
                ExecutorApi.executorParameters("https://evil.example.com", ExecutorApi.PROJECT_ID, ExecutorApi.REF),
                Map.of());

        post("start", body, DEFAULT_TOKEN)
                .then().statusCode(400)
                .body(containsString("is not allowed"));

        assertTrue(stub.requests().isEmpty());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: A missing token is refused
     *   When DevOps starts the task without the header x-odm-token
     *   Then the response is 400 naming x-odm-token
     */
    @Test
    void whenTokenMissingThen400() {
        post("start", startBody(Map.of()), null)
                .then().statusCode(400)
                .body(containsString("x-odm-token"));

        assertTrue(stub.requests().isEmpty());
    }

    /**
     * Feature: Start a GitLab pipeline
     *
     * Scenario: GitLab rejects the ref
     *   Given GitLab answers pipeline creation with 400 "Reference not found"
     *   When DevOps starts the task
     *   Then the response is 400 containing "Reference not found"
     */
    @Test
    void whenGitLabRejectsRefThen400WithMessage() {
        stub.respondJson("POST", CREATE_PIPELINE, 400, "{\"message\":\"Reference not found\"}");

        post("start", startBody(Map.of()), DEFAULT_TOKEN)
                .then().statusCode(400)
                .body(containsString("Reference not found"));
    }
}
