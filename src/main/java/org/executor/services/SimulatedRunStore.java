package org.executor.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.executor.config.SimulationProperties;
import org.executor.exceptions.ResourceConflictException;
import org.executor.exceptions.NotFoundException;
import org.executor.resources.ExecutorParametersRes;
import org.executor.resources.GitRefRes;
import org.executor.resources.DataProductRepoRes;
import org.executor.resources.TaskLogsRes;
import org.executor.resources.TaskStartCommandRes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SimulatedRunStore {

    static final String STATUS_RUNNING = "RUNNING";
    static final String STATUS_SUCCEEDED = "SUCCEEDED";
    static final String STATUS_FAILED = "FAILED";
    static final String STATUS_CANCELED = "CANCELED";

    private static final String OUTCOME_PARAMETER = "starter.outcome";
    private static final String DURATION_PARAMETER = "starter.durationSeconds";

    private final ConcurrentHashMap<String, SimulatedRun> runs = new ConcurrentHashMap<>();

    private final SimulationProperties simulationProperties;

    @Inject
    public SimulatedRunStore(SimulationProperties simulationProperties) {
        this.simulationProperties = simulationProperties;
    }

    public String start(TaskStartCommandRes request, List<String> headerNames) {
        String providerRunId = "starter-" + UUID.randomUUID();
        Instant startedAt = Instant.now();
        SimulatedRun run = new SimulatedRun(
                providerRunId,
                startedAt,
                resolveDuration(request),
                resolveOutcome(request),
                request,
                copyHeaderNames(headerNames),
                false
        );
        runs.put(providerRunId, run);
        return providerRunId;
    }

    public String status(String providerRunId) {
        SimulatedRun run = require(providerRunId);
        if (run.canceled()) {
            return STATUS_CANCELED;
        }
        if (!ended(run)) {
            return STATUS_RUNNING;
        }
        return run.plannedOutcome();
    }

    public void cancel(String providerRunId) {
        runs.compute(providerRunId, (id, run) -> {
            if (run == null) {
                throw new NotFoundException("Unknown providerRunId");
            }
            if (ended(run) && !run.canceled()) {
                throw new ResourceConflictException("Run has already ended");
            }
            return run.withCanceled();
        });
    }

    public TaskLogsRes logs(String providerRunId) {
        SimulatedRun run = require(providerRunId);
        TaskLogsRes logs = new TaskLogsRes();
        logs.setContent(render(run));
        logs.setGeneratedAt(new Date());
        return logs;
    }

    private static boolean ended(SimulatedRun run) {
        Instant end = run.startedAt().plus(run.plannedDuration());
        return !Instant.now().isBefore(end);
    }

    private SimulatedRun require(String providerRunId) {
        SimulatedRun run = runs.get(providerRunId);
        if (run == null) {
            throw new NotFoundException("Unknown providerRunId");
        }
        return run;
    }

    private Duration resolveDuration(TaskStartCommandRes request) {
        String raw = pipelineParameter(request, DURATION_PARAMETER);
        if (raw != null && raw.matches("[0-9]+")) {
            try {
                return Duration.ofSeconds(Long.parseLong(raw));
            } catch (NumberFormatException ex) {
                return configuredDuration();
            }
        }
        return configuredDuration();
    }

    private Duration configuredDuration() {
        Duration configured = simulationProperties.runDuration();
        if (configured == null) {
            return Duration.ofSeconds(5);
        }
        return configured;
    }

    private String resolveOutcome(TaskStartCommandRes request) {
        String requested = canonicalOutcome(pipelineParameter(request, OUTCOME_PARAMETER));
        if (requested != null) {
            return requested;
        }
        String configured = simulationProperties.outcome();
        String canonical = canonicalOutcome(configured);
        if (canonical != null) {
            return canonical;
        }
        if (configured == null || configured.isBlank()) {
            return STATUS_SUCCEEDED;
        }
        return configured;
    }

    private String canonicalOutcome(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (STATUS_SUCCEEDED.equals(normalized) || STATUS_FAILED.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String pipelineParameter(TaskStartCommandRes request, String key) {
        if (request == null || request.getPipelineParameters() == null) {
            return null;
        }
        return request.getPipelineParameters().get(key);
    }

    private List<String> copyHeaderNames(List<String> headerNames) {
        if (headerNames == null || headerNames.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String name : headerNames) {
            if (name != null) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    private String render(SimulatedRun run) {
        TaskStartCommandRes request = run.request();
        ExecutorParametersRes parameters = request == null ? null : request.getExecutorParameters();
        DataProductRepoRes repository = parameters == null ? null : parameters.getDataProductRepo();
        GitRefRes ref = parameters == null ? null : parameters.getRef();
        Instant endedAt = run.startedAt().plus(run.plannedDuration());
        return String.join("\n",
                "providerRunId: " + run.providerRunId(),
                "repositoryName: " + text(repository == null ? null : repository.getName()),
                "repositoryKey: " + text(parameters == null ? null : parameters.getRepositoryKey()),
                "refName: " + text(ref == null ? null : ref.getName()),
                "refType: " + text(ref == null ? null : ref.getType()),
                "pipelineIdentifier: " + text(parameters == null ? null : parameters.getPipelineIdentifier()),
                "pipelineParameters: " + formatParameters(request == null ? null : request.getPipelineParameters()),
                "secretHeaderNames: " + String.join(", ", run.secretHeaderNames()),
                "startedAt: " + run.startedAt(),
                "endedAt: " + endedAt,
                "outcome: " + run.plannedOutcome()
        );
    }

    private String formatParameters(Map<String, String> pipelineParameters) {
        if (pipelineParameters == null || pipelineParameters.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : pipelineParameters.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    public record SimulatedRun(
            String providerRunId,
            Instant startedAt,
            Duration plannedDuration,
            String plannedOutcome,
            TaskStartCommandRes request,
            List<String> secretHeaderNames,
            boolean canceled
    ) {
        SimulatedRun withCanceled() {
            return new SimulatedRun(
                    providerRunId,
                    startedAt,
                    plannedDuration,
                    plannedOutcome,
                    request,
                    secretHeaderNames,
                    true
            );
        }
    }
}
