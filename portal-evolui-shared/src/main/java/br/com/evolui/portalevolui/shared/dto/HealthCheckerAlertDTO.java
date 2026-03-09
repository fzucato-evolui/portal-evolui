package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.enums.HealthCheckerAlertTypeEnum;

public class HealthCheckerAlertDTO {
    protected HealthCheckerAlertTypeEnum alertType;
    protected boolean health;
    protected String error;

    public boolean isHealth() {
        return health;
    }

    public void setHealth(boolean health) {
        this.health = health;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public HealthCheckerAlertTypeEnum getAlertType() {
        return alertType;
    }

    public void setAlertType(HealthCheckerAlertTypeEnum alertType) {
        this.alertType = alertType;
    }
}
