package org.opendatamesh.platform.up.executor.gitlab.tasks.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataProductRepoRes {

    @Schema(description = "Provider type of the repository", example = "GITLAB")
    private String providerType;

    @Schema(description = "Base URL of the GitLab instance", example = "https://gitlab.com")
    private String providerBaseUrl;

    @Schema(description = "Provider identifier of the project; may be a numeric id or a URL-encodable path", example = "group/project")
    private String externalIdentifier;

    @Schema(description = "Repository name")
    private String name;

    @Schema(description = "Identifier of the repository owner")
    private String ownerId;

    @Schema(description = "Type of the repository owner")
    private String ownerType;

    @Schema(description = "HTTP URL of the remote repository")
    private String remoteUrlHttp;

    @Schema(description = "Default branch of the repository")
    private String defaultBranch;

    public DataProductRepoRes() {
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderBaseUrl() {
        return providerBaseUrl;
    }

    public void setProviderBaseUrl(String providerBaseUrl) {
        this.providerBaseUrl = providerBaseUrl;
    }

    public String getExternalIdentifier() {
        return externalIdentifier;
    }

    public void setExternalIdentifier(String externalIdentifier) {
        this.externalIdentifier = externalIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getRemoteUrlHttp() {
        return remoteUrlHttp;
    }

    public void setRemoteUrlHttp(String remoteUrlHttp) {
        this.remoteUrlHttp = remoteUrlHttp;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}
