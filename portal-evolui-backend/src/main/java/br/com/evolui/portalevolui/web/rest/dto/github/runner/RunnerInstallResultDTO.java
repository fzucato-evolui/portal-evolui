package br.com.evolui.portalevolui.web.rest.dto.github.runner;

public class RunnerInstallResultDTO {

    private boolean success;
    private String message;
    private String detail;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
