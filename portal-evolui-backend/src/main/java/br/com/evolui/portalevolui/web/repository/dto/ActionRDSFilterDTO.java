package br.com.evolui.portalevolui.web.repository.dto;

import br.com.evolui.portalevolui.web.beans.enums.ActionRDSTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;

import java.util.Calendar;

public class ActionRDSFilterDTO {
    private Long id;
    private ActionRDSTypeEnum actionType;
    private Calendar requestDateFrom;
    private Calendar requestDateTo;
    private String userName;
    private String userEmail;
    private String rds;
    private GithubActionStatusEnum status;
    private GithubActionConclusionEnum conclusion;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActionRDSTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(ActionRDSTypeEnum actionType) {
        this.actionType = actionType;
    }

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

    public String getRds() {
        return rds;
    }

    public void setRds(String rds) {
        this.rds = rds;
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
}
