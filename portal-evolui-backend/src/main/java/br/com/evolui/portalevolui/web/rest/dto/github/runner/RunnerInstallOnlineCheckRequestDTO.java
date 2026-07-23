package br.com.evolui.portalevolui.web.rest.dto.github.runner;

/**
 * Pedido do client Go ao modal: verificar via API GitHub se o runner com o nome dado ficou online.
 * O modal executa o polling contra {@code GET /api/admin/github/runner/} e responde com {@link RunnerInstallOnlineCheckResponseDTO}.
 */
public class RunnerInstallOnlineCheckRequestDTO {

    private String runnerName;

    public String getRunnerName() {
        return runnerName;
    }

    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }
}
