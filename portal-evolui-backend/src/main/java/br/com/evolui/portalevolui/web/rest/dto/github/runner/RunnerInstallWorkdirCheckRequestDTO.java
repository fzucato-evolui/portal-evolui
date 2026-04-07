package br.com.evolui.portalevolui.web.rest.dto.github.runner;

public class RunnerInstallWorkdirCheckRequestDTO {

    /** Onde extrair e configurar o runner (obrigatório para a verificação). */
    private String runnerInstallFolder;
    /** Opcional; vazio significa uso de {@code _work} relativo à pasta de instalação. */
    private String workFolder;

    public String getRunnerInstallFolder() {
        return runnerInstallFolder;
    }

    public void setRunnerInstallFolder(String runnerInstallFolder) {
        this.runnerInstallFolder = runnerInstallFolder;
    }

    public String getWorkFolder() {
        return workFolder;
    }

    public void setWorkFolder(String workFolder) {
        this.workFolder = workFolder;
    }
}
