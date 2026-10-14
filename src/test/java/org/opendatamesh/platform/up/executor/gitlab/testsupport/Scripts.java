package org.opendatamesh.platform.up.executor.gitlab.testsupport;

/**
 * JSON fixtures for GitLab responses that several tests script the same way.
 */
public final class Scripts {

    private Scripts() {
    }

    public static String pipeline(long id, String status) {
        return "{\"id\":" + id + ",\"status\":\"" + status + "\"}";
    }

    public static String job(long id, String name, String stage, String status) {
        return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"stage\":\"" + stage + "\",\"status\":\"" + status + "\"}";
    }

    public static String jobs(String... jobs) {
        return "[" + String.join(",", jobs) + "]";
    }
}
