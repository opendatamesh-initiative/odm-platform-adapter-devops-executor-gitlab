package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public enum RunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public static RunStatus fromGitLab(String status) {
        if (status == null) {
            return RUNNING;
        }
        return switch (status) {
            case "success" -> SUCCEEDED;
            case "failed", "skipped" -> FAILED;
            case "canceled" -> CANCELED;
            default -> RUNNING;
        };
    }

    public static boolean isCanceling(String status) {
        return "canceling".equals(status);
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
}
