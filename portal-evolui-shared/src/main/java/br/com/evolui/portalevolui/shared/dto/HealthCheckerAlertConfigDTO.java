package br.com.evolui.portalevolui.shared.dto;

public class HealthCheckerAlertConfigDTO {
    private Integer maxPercentual;
    private boolean sendNotification;

    public Integer getMaxPercentual() {
        return maxPercentual;
    }

    public void setMaxPercentual(Integer maxPercentual) {
        this.maxPercentual = maxPercentual;
    }

    public boolean isSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
}
