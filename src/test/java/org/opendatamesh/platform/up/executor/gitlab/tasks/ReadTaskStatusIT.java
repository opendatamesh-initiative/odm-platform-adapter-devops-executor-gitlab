package org.opendatamesh.platform.up.executor.gitlab.tasks;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStubResource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.Scripts;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.DEFAULT_TOKEN;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.post;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.runBody;

@QuarkusTest
@QuarkusTestResource(GitLabStubResource.class)
class ReadTaskStatusIT {

    private static final String PIPELINE_42 = "/api/v4/projects/123/pipelines/42";

    private GitLabStub stub;

    @BeforeEach
    void resetStub() {
        stub = GitLabStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Read a GitLab pipeline status
     *
     * Scenario Outline: GitLab statuses map to the common vocabulary
     *   Given the GitLab pipeline 42 has status "<gitlab>"
     *   When DevOps reads the status of providerRunId "42"
     *   Then the status is "<executor>"
     *   Examples:
     *     | gitlab    | executor  |
     *     | running   | RUNNING   |
     *     | manual    | RUNNING   |
     *     | canceling | RUNNING   |
     *     | success   | SUCCEEDED |
     *     | failed    | FAILED    |
     *     | skipped   | FAILED    |
     *     | canceled  | CANCELED  |
     *     | brand_new | RUNNING   |
     */
    @ParameterizedTest(name = "GitLab status {0} maps to {1}")
    @CsvSource({
            "running,RUNNING",
            "manual,RUNNING",
            "canceling,RUNNING",
            "success,SUCCEEDED",
            "failed,FAILED",
            "skipped,FAILED",
            "canceled,CANCELED",
            "brand_new,RUNNING"
    })
    void whenGitLabStatusThenMappedStatus(String gitLabStatus, String executorStatus) {
        stub.respondJson("GET", PIPELINE_42, 200, Scripts.pipeline(42, gitLabStatus));

        post("status", runBody("42"), DEFAULT_TOKEN)
                .then().statusCode(200)
                .body("providerRunId", equalTo("42"))
                .body("status", equalTo(executorStatus));
    }

    /**
     * Feature: Read a GitLab pipeline status
     *
     * Scenario: Status without executorParameters is refused
     *   When DevOps reads a status with a body that has no executorParameters
     *   Then the response is 400 naming executorParameters
     */
    @Test
    void whenExecutorParametersMissingThen400() {
        post("status", Map.of("providerRunId", "42"), DEFAULT_TOKEN)
                .then().statusCode(400)
                .body(containsString("executorParameters"));

        assertTrue(stub.requests().isEmpty());
    }

    /**
     * Feature: Read a GitLab pipeline status
     *
     * Scenario: An unknown pipeline is not found
     *   Given GitLab answers 404 for pipeline 99
     *   When DevOps reads the status of providerRunId "99"
     *   Then the response is 404
     */
    @Test
    void whenPipelineUnknownThen404() {
        stub.respondText("GET", "/api/v4/projects/123/pipelines/99", 404, "404 Pipeline Not Found");

        post("status", runBody("99"), DEFAULT_TOKEN)
                .then().statusCode(404);
    }
}
