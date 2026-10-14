package org.opendatamesh.platform.up.executor.gitlab.tasks;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStubResource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.Scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.DEFAULT_TOKEN;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.post;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.runBody;

@QuarkusTest
@QuarkusTestResource(GitLabStubResource.class)
class CancelTaskIT {

    private static final String PIPELINE_42 = "/api/v4/projects/123/pipelines/42";
    private static final String CANCEL_42 = "/api/v4/projects/123/pipelines/42/cancel";

    private GitLabStub stub;

    @BeforeEach
    void resetStub() {
        stub = GitLabStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Cancel a GitLab pipeline
     *
     * Scenario: A running pipeline is canceled
     *   Given pipeline 42 has status "running"
     *   When DevOps cancels providerRunId "42"
     *   Then the response is 200
     *   And GitLab receives POST .../pipelines/42/cancel
     */
    @Test
    void whenPipelineRunningThenCancelPosted() {
        stub.respondJson("GET", PIPELINE_42, 200, Scripts.pipeline(42, "running"));

        post("cancel", runBody("42"), DEFAULT_TOKEN)
                .then().statusCode(200);

        assertEquals(1, stub.requestsTo("POST", CANCEL_42).size());
    }

    /**
     * Feature: Cancel a GitLab pipeline
     *
     * Scenario: An ended pipeline is a conflict
     *   Given pipeline 42 has status "success"
     *   When DevOps cancels providerRunId "42"
     *   Then the response is 409
     *   And GitLab receives no cancel request
     */
    @Test
    void whenPipelineEndedThen409AndNoCancel() {
        stub.respondJson("GET", PIPELINE_42, 200, Scripts.pipeline(42, "success"));

        post("cancel", runBody("42"), DEFAULT_TOKEN)
                .then().statusCode(409);

        assertTrue(stub.requestsTo("POST", CANCEL_42).isEmpty());
    }

    /**
     * Feature: Cancel a GitLab pipeline
     *
     * Scenario: A pipeline already canceling is accepted without a new cancel
     *   Given pipeline 42 has status "canceling"
     *   When DevOps cancels providerRunId "42"
     *   Then the response is 200
     *   And GitLab receives no cancel request
     */
    @Test
    void whenPipelineCancelingThen200AndNoCancel() {
        stub.respondJson("GET", PIPELINE_42, 200, Scripts.pipeline(42, "canceling"));

        post("cancel", runBody("42"), DEFAULT_TOKEN)
                .then().statusCode(200);

        assertTrue(stub.requestsTo("POST", CANCEL_42).isEmpty());
    }
}
