package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputSpecLookup;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.SelectedInput;

import java.util.List;

interface StartTaskGitLabOutboundPort {

    String findProjectPath(GitLabAccess access);

    InputSpecLookup findDeclaredInputs(GitLabAccess access, String projectPath, String ref);

    void waitBeforeNextInputSpecRead();

    int maxInputSpecReads();

    String createPipeline(GitLabAccess access, String ref, List<SelectedInput> inputs);
}
