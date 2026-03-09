package br.com.evolui.portalevolui.web.repository.dto.cicd;

import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;

import java.util.Calendar;

public class CICDFilterDTO {
    private Calendar requestDateFrom;
    private Calendar requestDateTo;
    private String userName;
    private String userEmail;
    private CompileTypeEnum compileType;
    private GithubActionStatusEnum status;
    private GithubActionConclusionEnum conclusion;
    private String version;

    public Calendar getRequestDateFrom() {
        return requestDateFrom;
    }

    public void setRequestDateFrom(Calendar requestDateFrom) {
        this.requestDateFrom = requestDateFrom;
    }

    public Calendar getRequestDateTo() {
        return requestDateTo;
    }

    public void setRequestDateTo(Calendar requestDateTo) {
        this.requestDateTo = requestDateTo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public CompileTypeEnum getCompileType() {
        return compileType;
    }

    public void setCompileType(CompileTypeEnum compileType) {
        this.compileType = compileType;
    }

    public GithubActionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GithubActionStatusEnum status) {
        this.status = status;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
