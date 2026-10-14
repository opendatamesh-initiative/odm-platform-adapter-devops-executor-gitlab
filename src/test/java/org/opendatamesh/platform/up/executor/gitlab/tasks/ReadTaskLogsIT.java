package org.opendatamesh.platform.up.executor.gitlab.tasks;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStubResource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.Scripts;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.DEFAULT_TOKEN;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.post;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.runBody;

@QuarkusTest
@QuarkusTestResource(GitLabStubResource.class)
class ReadTaskLogsIT {

    private static final String JOBS_OF_42 = "/api/v4/projects/123/pipelines/42/jobs";
    private static final String TRACE_OF_JOB_7 = "/api/v4/projects/123/jobs/7/trace";
    private static final String TRACE_OF_JOB_8 = "/api/v4/projects/123/jobs/8/trace";
    private static final String BUILD_HEADER = "=== Job build (id 7, stage build, status success) ===\n";
    private static final String DEPLOY_HEADER = "=== Job deploy (id 8, stage deploy, status success) ===\n";
    private static final int DEFAULT_JOB_LOG_LIMIT = 1024 * 1024;

    private GitLabStub stub;

    @BeforeEach
    void resetStub() {
        stub = GitLabStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Read GitLab pipeline logs
     *
     * Scenario: Job logs are aggregated in execution order
     *   Given pipeline 42 has jobs "build" (id 7) and "deploy" (id 8)
     *   And job 8 has no log
     *   When DevOps reads the logs of providerRunId "42"
     *   Then the content shows the "build" header and log first, then the "deploy" header with "(no log available)"
     *   And generatedAt is set
     *   And ANSI colour codes are removed
     */
    @Test
    void whenReadingLogsThenJobsAggregatedInOrder() {
        // Returned out of id order on purpose: the executor sorts the jobs by id.
        stub.respondJson("GET", JOBS_OF_42, 200, Scripts.jobs(
                Scripts.job(8, "deploy", "deploy", "success"),
                Scripts.job(7, "build", "build", "success")));
        stub.respondText("GET", TRACE_OF_JOB_7, 200,
                "\u001B[0Ksection_start:1700000000:build_script\r\u001B[0K\u001B[32mcompiling\u001B[0m\nbuild done\n");
        stub.respondText("GET", TRACE_OF_JOB_8, 404, "");

        Response response = post("logs", runBody("42"), DEFAULT_TOKEN);

        response.then().statusCode(200);
        String content = response.jsonPath().getString("content");
        int buildAt = content.indexOf(BUILD_HEADER);
        int deployAt = content.indexOf(DEPLOY_HEADER);
        assertTrue(buildAt >= 0, content);
        assertTrue(deployAt > buildAt, content);
        assertTrue(content.substring(buildAt, deployAt).endsWith("compiling\nbuild done\n"), content);
        assertTrue(content.endsWith(DEPLOY_HEADER + "(no log available)\n"), content);
        assertFalse(content.contains("\u001B"), "ANSI escape codes must be removed");
        assertFalse(content.contains("section_start"), "GitLab section markers must be removed");
        assertTrue(response.jsonPath().get("generatedAt") != null);
    }

    /**
     * Feature: Read GitLab pipeline logs
     *
     * Scenario: An oversized job log keeps its tail
     *   Given job 7 has a log larger than the per-job limit
     *   When DevOps reads the logs
     *   Then the content for job 7 starts with a truncation marker and ends with the last line of the log
     */
    @Test
    void whenJobLogTooLargeThenTailKeptWithMarker() {
        stub.respondJson("GET", JOBS_OF_42, 200, Scripts.jobs(Scripts.job(7, "build", "build", "success")));
        stub.respondText("GET", TRACE_OF_JOB_7, 200, oversizedLog());

        Response response = post("logs", runBody("42"), DEFAULT_TOKEN);

        response.then().statusCode(200);
        String content = response.jsonPath().getString("content");
        assertTrue(content.startsWith(BUILD_HEADER + "[... "), content.substring(0, 200));
        assertTrue(content.matches("(?s)=== Job build .*?\\n\\[\\.\\.\\. \\d+ bytes truncated \\.\\.\\.\\]\\n.*"),
                "truncation marker expected after the header");
        assertTrue(content.endsWith("LAST LINE OF THE LOG\n"));
        assertFalse(content.contains("line 0000000 "), "the head of the log must be dropped");
        assertTrue(content.getBytes(StandardCharsets.UTF_8).length <= DEFAULT_JOB_LOG_LIMIT + 1024,
                "content must stay within the per-job limit plus header and marker");
        assertEquals(1, content.split("=== Job ", -1).length - 1);
    }

    private static String oversizedLog() {
        StringBuilder log = new StringBuilder();
        for (int line = 0; log.length() < DEFAULT_JOB_LOG_LIMIT + 100_000; line++) {
            log.append(String.format("line %07d ", line)).append("x".repeat(40)).append('\n');
        }
        log.append("LAST LINE OF THE LOG\n");
        return log.toString();
    }
}
