package org.executor.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorParametersRes {

    private String repositoryKey;

    private DataProductRepoRes dataProductRepo;

    private GitRefRes ref;

    private String pipelineIdentifier;

    public ExecutorParametersRes() {
    }

    public String getRepositoryKey() {
        return repositoryKey;
    }

    public void setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
    }

    public DataProductRepoRes getDataProductRepo() {
        return dataProductRepo;
    }

    public void setDataProductRepo(DataProductRepoRes dataProductRepo) {
        this.dataProductRepo = dataProductRepo;
    }

    public GitRefRes getRef() {
        return ref;
    }

    public void setRef(GitRefRes ref) {
        this.ref = ref;
    }

    public String getPipelineIdentifier() {
        return pipelineIdentifier;
    }

    public void setPipelineIdentifier(String pipelineIdentifier) {
        this.pipelineIdentifier = pipelineIdentifier;
    }
}
