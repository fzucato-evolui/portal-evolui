package br.com.evolui.portalevolui.web.rest.dto.github.runner;

/**
 * Resposta do modal ao client Go com o resultado de um ciclo do polling REST contra a API GitHub
 * (mediado por {@code GET /api/admin/github/runner/}). O Go usa {@code online} para confirmar a instalação;
 * {@code exhausted=true} indica que o modal desistiu após esgotar as tentativas.
 */
public class RunnerInstallOnlineCheckResponseDTO {

    private boolean found;
    private boolean online;
    private boolean busy;
    private String status;
    private int attempt;
    private boolean exhausted;

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }
}
