package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.web.rest.dto.enums.AWSInstanceRunnerTypeEnum;

public class AWSRunnerConfigDTO {
    private String id;
    private AWSInstanceRunnerTypeEnum instanceType;
    private Long runnerGithubId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AWSInstanceRunnerTypeEnum getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(AWSInstanceRunnerTypeEnum instanceType) {
        this.instanceType = instanceType;
    }

    public Long getRunnerGithubId() {
        return runnerGithubId;
    }

    public void setRunnerGithubId(Long runnerGithubId) {
        this.runnerGithubId = runnerGithubId;
    }
}
