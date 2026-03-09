package br.com.evolui.portalevolui.web.repository.dto;

import br.com.evolui.portalevolui.web.beans.enums.AWSActionTypeEnum;

import java.util.Calendar;

public class LogAWSActionFilterDTO {
    private Long id;
    private Calendar logDateFrom;
    private Calendar logDateTo;
    private String userName;
    private String userEmail;
    private AWSActionTypeEnum actionType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Calendar getLogDateFrom() {
        return logDateFrom;
    }

    public void setLogDateFrom(Calendar logDateFrom) {
        this.logDateFrom = logDateFrom;
    }

    public Calendar getLogDateTo() {
        return logDateTo;
    }

    public void setLogDateTo(Calendar logDateTo) {
        this.logDateTo = logDateTo;
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

    public AWSActionTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(AWSActionTypeEnum actionType) {
        this.actionType = actionType;
    }
}
