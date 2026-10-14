package org.opendatamesh.platform.up.executor.gitlab.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scriptable fake of the GitLab endpoints used by the executor, served by the JDK HTTP server.
 *
 * <p>Every request is recorded (method, path with query, body and the {@code PRIVATE-TOKEN} value) before it is
 * answered. Responses are scripted per method and path: when several scripted routes match a request, the last one
 * registered wins, so a test can override a default. A path prefix matches the path itself and anything below it on a
 * segment boundary, so {@code /pipeline} does not match {@code /pipelines/42}.
 *
 * <p>Defaults installed by {@link #reset()}:
 * <ul>
 *   <li>{@code GET /api/v4/projects/{id}} returns the project with path {@code group/project};</li>
 *   <li>{@code POST /api/v4/projects/{id}/pipeline} returns pipeline 42 with status {@code created};</li>
 *   <li>{@code POST /api/v4/projects/{id}/pipelines/{pid}/cancel} returns an empty object.</li>
 * </ul>
 * Every other unscripted request is answered with 404.
 *
 * <p>The GraphQL endpoint {@code POST /api/graphql} is answered by the stub itself: with the declared inputs, or, while
 * the cold-cache mode is active, with GitLab's "Failed to retrieve pipeline inputs from cache" error.
 */
public final class GitLabStub implements AutoCloseable {

    public static final String CACHE_MISS_MESSAGE = "Failed to retrieve pipeline inputs from cache";
    public static final String PROJECT_PATH_WITH_NAMESPACE = "group/project";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String GRAPHQL_PATH = "/api/graphql";
    private static final String JSON_TYPE = "application/json";
    private static final String TEXT_TYPE = "text/plain; charset=utf-8";

    /** One input declared by the CI configuration at a ref. */
    public record DeclaredInputSpec(String name, String type, boolean required) {
    }

    /** One request received by the stub. {@code pathWithQuery} is the raw path, followed by {@code ?query} if any. */
    public record Recorded(String method, String pathWithQuery, String body, String privateToken) {

        public String path() {
            int query = pathWithQuery.indexOf('?');
            return query < 0 ? pathWithQuery : pathWithQuery.substring(0, query);
        }
    }

    private record Reply(int status, String contentType, byte[] body) {
    }

    private record Route(String method, Predicate<String> path, Reply reply) {
    }

    private final HttpServer server;
    private final List<Route> routes = new ArrayList<>();
    private final List<Recorded> recorded = new ArrayList<>();
    private List<DeclaredInputSpec> declaredInputs = new ArrayList<>();
    private int coldInputAnswers;
    private boolean coldInputAlways;

    public GitLabStub() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/", this::handle);
        this.server.start();
        reset();
    }

    /** Base URL of the stub, for example {@code http://127.0.0.1:41234}. */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Scripts a JSON response for requests with this method whose path equals or lies below {@code pathPrefix}. */
    public synchronized void respondJson(String method, String pathPrefix, int status, String json) {
        routes.add(new Route(method, segmentPrefix(pathPrefix), json(status, json)));
    }

    /** Scripts a plain-text response for requests with this method whose path equals or lies below {@code pathPrefix}. */
    public synchronized void respondText(String method, String pathPrefix, int status, String text) {
        routes.add(new Route(method, segmentPrefix(pathPrefix), text(status, text)));
    }

    /** Sets the inputs answered by the GraphQL query once the cache is warm. */
    public synchronized void declareInputs(DeclaredInputSpec... inputs) {
        declaredInputs = new ArrayList<>(List.of(inputs));
    }

    /** The next {@code times} GraphQL input queries are answered with the cache-miss error; later ones succeed. */
    public synchronized void coldInputCacheTimes(int times) {
        coldInputAnswers = times;
        coldInputAlways = false;
    }

    /** Every GraphQL input query is answered with the cache-miss error. */
    public synchronized void coldInputCacheAlways() {
        coldInputAlways = true;
    }

    /** All requests received since the last {@link #reset()}, in arrival order. */
    public synchronized List<Recorded> requests() {
        return List.copyOf(recorded);
    }

    /** Requests with this method whose path equals or lies below {@code pathPrefix}. */
    public synchronized List<Recorded> requests(String method, String pathPrefix) {
        Predicate<String> matches = segmentPrefix(pathPrefix);
        return recorded.stream()
                .filter(r -> r.method().equals(method) && matches.test(r.path()))
                .toList();
    }

    /** Requests with this method whose path is exactly {@code path}. */
    public synchronized List<Recorded> requestsTo(String method, String path) {
        return recorded.stream()
                .filter(r -> r.method().equals(method) && r.path().equals(path))
                .toList();
    }

    /** Forgets all requests, scripted routes, declared inputs and cold-cache state, then installs the defaults. */
    public synchronized void reset() {
        routes.clear();
        recorded.clear();
        declaredInputs = new ArrayList<>();
        coldInputAnswers = 0;
        coldInputAlways = false;
        routes.add(new Route("GET", path -> path.matches("/api/v4/projects/[^/]+"),
                json(200, "{\"id\":123,\"path_with_namespace\":\"" + PROJECT_PATH_WITH_NAMESPACE + "\"}")));
        routes.add(new Route("POST", path -> path.matches("/api/v4/projects/[^/]+/pipeline"),
                json(201, "{\"id\":42,\"status\":\"created\"}")));
        routes.add(new Route("POST", path -> path.matches("/api/v4/projects/[^/]+/pipelines/[^/]+/cancel"),
                json(200, "{}")));
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String pathWithQuery = exchange.getRequestURI().getRawPath();
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) {
                pathWithQuery = pathWithQuery + "?" + query;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String token = exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN");
            Reply reply = answer(new Recorded(exchange.getRequestMethod(), pathWithQuery, body, token));
            exchange.getResponseHeaders().set("Content-Type", reply.contentType());
            if (reply.body().length == 0) {
                exchange.sendResponseHeaders(reply.status(), -1);
            } else {
                exchange.sendResponseHeaders(reply.status(), reply.body().length);
                exchange.getResponseBody().write(reply.body());
            }
        } finally {
            exchange.close();
        }
    }

    private synchronized Reply answer(Recorded request) {
        recorded.add(request);
        if ("POST".equals(request.method()) && GRAPHQL_PATH.equals(request.path())) {
            return inputsReply();
        }
        for (int i = routes.size() - 1; i >= 0; i--) {
            Route route = routes.get(i);
            if (route.method().equals(request.method()) && route.path().test(request.path())) {
                return route.reply();
            }
        }
        return text(404, "no scripted response for " + request.method() + " " + request.path());
    }

    private Reply inputsReply() {
        if (coldInputAlways || coldInputAnswers > 0) {
            if (!coldInputAlways) {
                coldInputAnswers--;
            }
            return json(200, "{\"errors\":[{\"message\":\"" + CACHE_MISS_MESSAGE + "\"}]}");
        }
        ArrayNode items = JSON.createArrayNode();
        for (DeclaredInputSpec input : declaredInputs) {
            items.addObject()
                    .put("name", input.name())
                    .put("type", input.type())
                    .put("required", input.required());
        }
        ObjectNode root = JSON.createObjectNode();
        root.putObject("data").putObject("project").set("ciPipelineCreationInputs", items);
        return json(200, root.toString());
    }

    private static Predicate<String> segmentPrefix(String pathPrefix) {
        return path -> path.equals(pathPrefix) || path.startsWith(pathPrefix + "/");
    }

    private static Reply json(int status, String body) {
        return new Reply(status, JSON_TYPE, body.getBytes(StandardCharsets.UTF_8));
    }

    private static Reply text(int status, String body) {
        return new Reply(status, TEXT_TYPE, body.getBytes(StandardCharsets.UTF_8));
    }
}
