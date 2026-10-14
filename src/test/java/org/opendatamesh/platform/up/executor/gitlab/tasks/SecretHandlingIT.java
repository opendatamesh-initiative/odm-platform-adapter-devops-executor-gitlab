package org.opendatamesh.platform.up.executor.gitlab.tasks;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStub.DeclaredInputSpec;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.GitLabStubResource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.Scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.post;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.runBody;
import static org.opendatamesh.platform.up.executor.gitlab.testsupport.ExecutorApi.startBody;

@QuarkusTest
@QuarkusTestResource(GitLabStubResource.class)
class SecretHandlingIT {

    private static final String SECRET = "super-secret-token";
    private static final String PIPELINE_42 = "/api/v4/projects/123/pipelines/42";

    private GitLabStub stub;

    @BeforeEach
    void resetStub() {
        stub = GitLabStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Statelessness and secrets
     *
     * Scenario: The token is never logged or returned
     *   Given the token "super-secret-token"
     *   When DevOps starts, reads status, reads logs and cancels a run
     *   Then no executor log line and no response body contains "super-secret-token"
     */
    @Test
    void whenCallingAllEndpointsThenTokenNeverLoggedOrReturned() {
        stub.declareInputs(new DeclaredInputSpec("env", "STRING", true));
        stub.respondJson("GET", PIPELINE_42, 200, Scripts.pipeline(42, "running"));
        stub.respondJson("GET", PIPELINE_42 + "/jobs", 200, Scripts.jobs(Scripts.job(7, "build", "build", "running")));
        stub.respondText("GET", "/api/v4/projects/123/jobs/7/trace", 200, "building\n");

        List<LogRecord> captured = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        capture.setLevel(Level.ALL);
        // Attached to the root so every appender-level record is seen. Only the executor's own package logger is
        // opened to DEBUG; the client-side wire loggers of the test HTTP client stay as configured.
        Logger root = Logger.getLogger("");
        Logger executor = Logger.getLogger("org.opendatamesh.platform.up.executor.gitlab");
        Level previousExecutorLevel = executor.getLevel();
        root.addHandler(capture);
        executor.setLevel(Level.ALL);

        List<String> responses = new ArrayList<>();
        try {
            responses.add(post("start", startBody(Map.of("env", "prod")), SECRET)
                    .then().statusCode(200).extract().asString());
            responses.add(post("status", runBody("42"), SECRET)
                    .then().statusCode(200).extract().asString());
            responses.add(post("logs", runBody("42"), SECRET)
                    .then().statusCode(200).extract().asString());
            responses.add(post("cancel", runBody("42"), SECRET)
                    .then().statusCode(200).extract().asString());
        } finally {
            root.removeHandler(capture);
            executor.setLevel(previousExecutorLevel);
        }

        assertTrue(captured.stream().anyMatch(r -> r.getMessage() != null
                && r.getMessage().contains("Started GitLab pipeline")),
                "the capture must see the executor's own log lines");
        for (LogRecord record : captured) {
            assertFalse(render(record).contains(SECRET),
                    "log record from " + record.getLoggerName() + " leaks the token: " + render(record));
        }
        for (String response : responses) {
            assertFalse(response.contains(SECRET), "response body leaks the token: " + response);
        }
    }

    private static String render(LogRecord record) {
        StringBuilder text = new StringBuilder();
        if (record.getMessage() != null) {
            text.append(record.getMessage());
        }
        if (record.getParameters() != null) {
            for (Object parameter : record.getParameters()) {
                text.append(' ').append(parameter);
            }
        }
        if (record.getThrown() != null) {
            text.append(' ').append(record.getThrown());
        }
        return text.toString();
    }
}
