package org.executor.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.executor.resources.ExecutorParametersRes;
import org.executor.resources.GitRefRes;
import org.executor.resources.DataProductRepoRes;
import org.executor.resources.TaskCancelCommandRes;
import org.executor.resources.TaskStartCommandRes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
class ExecutorTaskResourceTest {

    private static final String START = "/api/v2/up/executor/tasks/start";
    private static final String STATUS = "/api/v2/up/executor/tasks/status";
    private static final String LOGS = "/api/v2/up/executor/tasks/logs";
    private static final String CANCEL = "/api/v2/up/executor/tasks/cancel";

    private static final String REPOSITORY_KEY = "infra-repo";
    private static final String REPOSITORY_NAME = "sales-app";
    private static final String REF_NAME = "v1.2.0";
    private static final String REF_TYPE = "TAG";
    private static final String PIPELINE_IDENTIFIER = "build.yml";

    /**
     * Feature: Starter executor (repository odm-platform-adapter-devops-executor-starter)
     *
     * Scenario: A simulated run is running until its duration ends, then reports the planned outcome
     *   Given the default run duration is 200 ms and the default outcome is SUCCEEDED
     *   When a run is started and its status is read at once and again after 300 ms
     *   Then the status is RUNNING and then SUCCEEDED
     */
    @Test
    void whenRunStartedThenRunningThenPlannedOutcome() throws Exception {
        String providerRunId = startRun(Map.of());

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("providerRunId", org.hamcrest.Matchers.equalTo(providerRunId))
                .body("status", org.hamcrest.Matchers.equalTo("RUNNING"));

        Thread.sleep(300);

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("providerRunId", org.hamcrest.Matchers.equalTo(providerRunId))
                .body("status", org.hamcrest.Matchers.equalTo("SUCCEEDED"));
    }

    /**
     * Feature: Starter executor (repository odm-platform-adapter-devops-executor-starter)
     *
     * Scenario: Reserved pipeline parameters override the defaults
     *   Given a start request with pipeline parameters starter.outcome = FAILED and starter.durationSeconds = 0
     *   When its status is read
     *   Then the status is FAILED
     */
    @Test
    void whenReservedParametersThenOverrideDefaults() throws Exception {
        Map<String, String> pipelineParameters = new LinkedHashMap<>();
        pipelineParameters.put("starter.outcome", "FAILED");
        pipelineParameters.put("starter.durationSeconds", "0");

        String providerRunId = startRun(pipelineParameters);

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("providerRunId", org.hamcrest.Matchers.equalTo(providerRunId))
                .body("status", org.hamcrest.Matchers.equalTo("FAILED"));
    }

    /**
     * Feature: Starter executor (repository odm-platform-adapter-devops-executor-starter)
     *
     * Scenario: A start call without executor parameters is refused
     *   When the start body is an empty object
     *   Then the response is 400 and names executorParameters
     */
    @Test
    void whenStartOmitsExecutorParametersThenBadRequestNamesTheField() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post(START)
                .then()
                .statusCode(400)
                .body(containsString("executorParameters"));
    }

    /**
     * Feature: Starter executor (repository odm-platform-adapter-devops-executor-starter)
     *
     * Scenario: An unknown run id is not found
     *   When the status or the logs of an unknown providerRunId are read
     *   Then the response is 404
     */
    @Test
    void whenUnknownRunIdThenNotFound() {
        given()
                .queryParam("providerRunId", "starter-does-not-exist")
                .when().get(STATUS)
                .then()
                .statusCode(404);

        given()
                .queryParam("providerRunId", "starter-does-not-exist")
                .when().get(LOGS)
                .then()
                .statusCode(404);
    }

    /**
     * Feature: Starter executor (repository odm-platform-adapter-devops-executor-starter)
     *
     * Scenario: The log lists header names and never secret values
     *   Given a start request with the header x-odm-token = "s3cr3t"
     *   When the logs are read
     *   Then the log contains "x-odm-token" and does not contain "s3cr3t"
     */
    @Test
    void whenLogsReadThenHeaderNamesWithoutValues() throws Exception {
        Map<String, String> pipelineParameters = new LinkedHashMap<>();
        pipelineParameters.put("region", "eu-west");

        String providerRunId = startRun(pipelineParameters, "x-odm-token", "s3cr3t");

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(LOGS)
                .then()
                .statusCode(200)
                .body("generatedAt", org.hamcrest.Matchers.notNullValue())
                .body("content", containsString("x-odm-token"))
                .body("content", not(containsString("s3cr3t")))
                .body("content", containsString(providerRunId))
                .body("content", containsString(REPOSITORY_NAME))
                .body("content", containsString(REPOSITORY_KEY))
                .body("content", containsString(REF_NAME))
                .body("content", containsString(REF_TYPE))
                .body("content", containsString(PIPELINE_IDENTIFIER))
                .body("content", containsString("region=eu-west"))
                .body("content", containsString("SUCCEEDED"));
    }

    /**
     * Feature: Starter cancel
     *
     * Scenario: A running simulated run reports CANCELED after cancel
     *   Given a run whose duration has not elapsed
     *   When the executor is asked to cancel that run
     *   Then the next status read is CANCELED
     */
    @Test
    void whenRunCanceledThenStatusIsCanceled() throws Exception {
        Map<String, String> pipelineParameters = new LinkedHashMap<>();
        pipelineParameters.put("starter.durationSeconds", "60");

        String providerRunId = startRun(pipelineParameters);

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("status", org.hamcrest.Matchers.equalTo("RUNNING"));

        given()
                .contentType(ContentType.JSON)
                .body(cancelBody(providerRunId))
                .when().post(CANCEL)
                .then()
                .statusCode(200);

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("providerRunId", org.hamcrest.Matchers.equalTo(providerRunId))
                .body("status", org.hamcrest.Matchers.equalTo("CANCELED"));
    }

    /**
     * Feature: Starter cancel
     *
     * Scenario: Cancel of a finished run conflicts and leaves the status
     *   Given a run whose status is already SUCCEEDED
     *   When the executor is asked to cancel that run
     *   Then the response is 409
     *   And the next status read is SUCCEEDED
     */
    @Test
    void whenRunAlreadyEndedThenCancelConflicts() throws Exception {
        Map<String, String> pipelineParameters = new LinkedHashMap<>();
        pipelineParameters.put("starter.outcome", "SUCCEEDED");
        pipelineParameters.put("starter.durationSeconds", "0");

        String providerRunId = startRun(pipelineParameters);

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("status", org.hamcrest.Matchers.equalTo("SUCCEEDED"));

        given()
                .contentType(ContentType.JSON)
                .body(cancelBody(providerRunId))
                .when().post(CANCEL)
                .then()
                .statusCode(409)
                .body(containsString("Run has already ended"));

        given()
                .queryParam("providerRunId", providerRunId)
                .when().get(STATUS)
                .then()
                .statusCode(200)
                .body("providerRunId", org.hamcrest.Matchers.equalTo(providerRunId))
                .body("status", org.hamcrest.Matchers.equalTo("SUCCEEDED"));
    }

    /**
     * Feature: Starter cancel
     *
     * Scenario: Cancel of an unknown run is not found
     *   Given no run has that provider run id
     *   When the executor is asked to cancel it
     *   Then the response is 404
     */
    @Test
    void whenRunUnknownThenCancelIsNotFound() throws Exception {
        given()
                .contentType(ContentType.JSON)
                .body(cancelBody("starter-does-not-exist"))
                .when().post(CANCEL)
                .then()
                .statusCode(404)
                .body(containsString("Unknown providerRunId"));
    }

    private String startRun(Map<String, String> pipelineParameters, String headerName, String headerValue) throws Exception {
        var request = given()
                .contentType(ContentType.JSON)
                .body(body(pipelineParameters));
        if (headerName != null) {
            request.header(headerName, headerValue);
        }
        return request
                .when().post(START)
                .then()
                .statusCode(200)
                .body(not(containsString("s3cr3t")))
                .body("providerRunId", startsWith("starter-"))
                .extract()
                .path("providerRunId");
    }

    private String startRun(Map<String, String> pipelineParameters) throws Exception {
        return startRun(pipelineParameters, null, null);
    }

    private TaskCancelCommandRes cancelBody(String providerRunId) {
        TaskCancelCommandRes command = new TaskCancelCommandRes();
        command.setProviderRunId(providerRunId);
        return command;
    }

    private TaskStartCommandRes body(Map<String, String> pipelineParameters) {
        DataProductRepoRes repository = new DataProductRepoRes();
        repository.setProviderType("GITHUB");
        repository.setProviderBaseUrl("https://github.com");
        repository.setExternalIdentifier("812300");
        repository.setName(REPOSITORY_NAME);
        repository.setOwnerId("4411");
        repository.setOwnerType("ORGANIZATION");
        repository.setRemoteUrlHttp("https://github.com/acme/sales-app.git");
        repository.setDefaultBranch("main");

        GitRefRes ref = new GitRefRes();
        ref.setName(REF_NAME);
        ref.setType(REF_TYPE);

        ExecutorParametersRes executorParameters = new ExecutorParametersRes();
        executorParameters.setRepositoryKey(REPOSITORY_KEY);
        executorParameters.setDataProductRepo(repository);
        executorParameters.setRef(ref);
        executorParameters.setPipelineIdentifier(PIPELINE_IDENTIFIER);

        TaskStartCommandRes command = new TaskStartCommandRes();
        command.setExecutorParameters(executorParameters);
        command.setPipelineParameters(pipelineParameters);
        return command;
    }
}
