package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunStatusTest {

    @ParameterizedTest
    @CsvSource({
            "success,SUCCEEDED",
            "failed,FAILED",
            "skipped,FAILED",
            "canceled,CANCELED",
            "created,RUNNING",
            "waiting_for_resource,RUNNING",
            "preparing,RUNNING",
            "waiting_for_callback,RUNNING",
            "pending,RUNNING",
            "running,RUNNING",
            "scheduled,RUNNING",
            "canceling,RUNNING",
            "manual,RUNNING",
            "brand_new,RUNNING"
    })
    void fromGitLabMapsEachStatus(String gitLabStatus, RunStatus expected) {
        assertEquals(expected, RunStatus.fromGitLab(gitLabStatus));
    }

    @Test
    void fromGitLabMapsAbsentStatusToRunning() {
        assertEquals(RunStatus.RUNNING, RunStatus.fromGitLab(null));
    }

    @ParameterizedTest
    @CsvSource({
            "RUNNING,false",
            "SUCCEEDED,true",
            "FAILED,true",
            "CANCELED,true"
    })
    void isTerminalOnlyForEndedStatuses(RunStatus status, boolean terminal) {
        assertEquals(terminal, status.isTerminal());
    }

    @Test
    void isCancelingOnlyForCanceling() {
        assertTrue(RunStatus.isCanceling("canceling"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"running", "canceled", "success", "manual"})
    void isCancelingFalseForOtherStatuses(String gitLabStatus) {
        assertFalse(RunStatus.isCanceling(gitLabStatus));
    }

    @Test
    void isCancelingFalseForAbsentStatus() {
        assertFalse(RunStatus.isCanceling(null));
    }
}
