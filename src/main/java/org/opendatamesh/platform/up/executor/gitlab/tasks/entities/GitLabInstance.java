package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

public record GitLabInstance(String baseUrl) {

    public static Optional<GitLabInstance> parse(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(url.trim());
            if (!uri.isAbsolute() || uri.getHost() == null || !isHttpScheme(uri.getScheme())) {
                return Optional.empty();
            }
            return Optional.of(new GitLabInstance(normalise(uri)));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static String normalise(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
        return scheme + "://" + host + port + path;
    }
}
