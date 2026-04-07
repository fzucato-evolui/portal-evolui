package br.com.evolui.portalevolui.web.rest.dto.github.runner;

public class RunnerInstallWorkdirCheckResponseDTO {

    private boolean exists;
    private boolean writable;
    private String detail;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
