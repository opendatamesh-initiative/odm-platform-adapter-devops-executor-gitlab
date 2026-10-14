package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ServiceUnavailableException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.DeclaredInput;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputSpecLookup;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.SelectedInput;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class StartTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(StartTask.class);

    private final StartTaskCommand command;
    private final StartTaskPresenter presenter;
    private final StartTaskInstanceOutboundPort instancePort;
    private final StartTaskGitLabOutboundPort gitLabPort;

    StartTask(StartTaskCommand command, StartTaskPresenter presenter,
              StartTaskInstanceOutboundPort instancePort, StartTaskGitLabOutboundPort gitLabPort) {
        this.command = command;
        this.presenter = presenter;
        this.instancePort = instancePort;
        this.gitLabPort = gitLabPort;
    }

    @Override
    public void execute() {
        validateCommand();
        GitLabAccess access = resolveAccess();
        List<DeclaredInput> declared = readDeclaredInputs(access);
        List<SelectedInput> selected = selectInputs(declared);
        String pipelineId = gitLabPort.createPipeline(access, refName(), selected);
        logger.info("Started GitLab pipeline {} in project {}", pipelineId, access.projectId());
        presenter.presentStarted(pipelineId);
    }

    private void validateCommand() {
        PipelineLocation location = command.location();
        if (isBlank(location.projectId())) {
            throw new BadRequestException("executorParameters.dataProductRepo.externalIdentifier is required");
        }
        if (isBlank(location.refName())) {
            throw new BadRequestException("executorParameters.ref.name is required");
        }
        if (isBlank(command.token())) {
            throw new BadRequestException("Header x-odm-token is required");
        }
    }

    private GitLabAccess resolveAccess() {
        PipelineLocation location = command.location();
        GitLabInstance instance = instancePort.resolveInstance(location.providerBaseUrl())
                .orElseThrow(() -> new BadRequestException(
                        "GitLab instance " + location.providerBaseUrl() + " is not allowed"));
        return new GitLabAccess(instance, location.projectId(), command.token());
    }

    private List<DeclaredInput> readDeclaredInputs(GitLabAccess access) {
        String projectPath = gitLabPort.findProjectPath(access);
        int maxReads = gitLabPort.maxInputSpecReads();
        for (int read = 1; read <= maxReads; read++) {
            InputSpecLookup lookup = gitLabPort.findDeclaredInputs(access, projectPath, refName());
            if (lookup.ready()) {
                return lookup.inputs();
            }
            if (read < maxReads) {
                gitLabPort.waitBeforeNextInputSpecRead();
            }
        }
        throw new ServiceUnavailableException(
                "GitLab has not computed the pipeline inputs for ref " + refName() + " yet; retry the task");
    }

    private List<SelectedInput> selectInputs(List<DeclaredInput> declared) {
        Map<String, String> parameters = parameters();
        rejectMissingRequired(declared, parameters);
        logDroppedParameters(declared, parameters);
        return selectDeclared(declared, parameters);
    }

    private Map<String, String> parameters() {
        Map<String, String> parameters = command.pipelineParameters();
        return parameters == null ? Map.of() : parameters;
    }

    private static void rejectMissingRequired(List<DeclaredInput> declared, Map<String, String> parameters) {
        List<String> missing = declared.stream()
                .filter(DeclaredInput::required)
                .map(DeclaredInput::name)
                .filter(name -> !parameters.containsKey(name))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("Missing required pipeline inputs: " + String.join(", ", missing));
        }
    }

    private static void logDroppedParameters(List<DeclaredInput> declared, Map<String, String> parameters) {
        Set<String> declaredNames = declaredNames(declared);
        List<String> dropped = parameters.keySet().stream()
                .filter(key -> !declaredNames.contains(key))
                .sorted()
                .toList();
        if (!dropped.isEmpty()) {
            logger.info("Dropped pipeline parameters not declared as GitLab inputs: {}", dropped);
        }
    }

    private static List<SelectedInput> selectDeclared(List<DeclaredInput> declared, Map<String, String> parameters) {
        return declared.stream()
                .filter(input -> parameters.containsKey(input.name()))
                .map(input -> new SelectedInput(input, parameters.get(input.name())))
                .toList();
    }

    private String refName() {
        return command.location().refName();
    }

    private static Set<String> declaredNames(List<DeclaredInput> declared) {
        return declared.stream().map(DeclaredInput::name).collect(Collectors.toSet());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
