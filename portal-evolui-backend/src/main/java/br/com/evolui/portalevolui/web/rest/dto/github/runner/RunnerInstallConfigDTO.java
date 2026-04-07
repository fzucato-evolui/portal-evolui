package br.com.evolui.portalevolui.web.rest.dto.github.runner;

/**
 * Configuração final enviada pelo browser ao client (após “Instalar agora” e obtenção do token via REST).
 */
public class RunnerInstallConfigDTO {

    private String runnerGroup;
    private String runnerName;
    private String runnerAlias;
    /**
     * Diretório na máquina alvo onde o pacote {@code actions-runner} é extraído e onde rodam {@code config.sh|config.cmd}
     * (ex.: {@code /opt/actions-runner}, {@code C:\actions-runner}).
     */
    private String runnerInstallFolder;
    /**
     * Argumento {@code --work} do configurador: onde os jobs executam. Se vazio, o client usa {@code _work}
     * (relativo à pasta de instalação do runner), como no padrão do GitHub.
     */
    private String workFolder;
    private boolean installAsService;
    private String serviceAccountUser;
    private String serviceAccountPassword;
    private String registrationToken;
    /**
     * URL da organização no GitHub para {@code config.sh|config.cmd --url} (ex.: {@code https://github.com/minha-org}).
     * Preenchida pelo portal a partir da configuração GitHub ({@code owner}); o client Go não precisa de flag externa.
     */
    private String githubOrganizationUrl;
    /** URL do pacote oficial actions/runner (zip ou tar.gz) já resolvida pelo portal. */
    private String actionsRunnerDownloadUrl;
    private String actionsRunnerVersion;

    public String getRunnerGroup() {
        return runnerGroup;
    }

    public void setRunnerGroup(String runnerGroup) {
        this.runnerGroup = runnerGroup;
    }

    public String getRunnerName() {
        return runnerName;
    }

    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }

    public String getRunnerAlias() {
        return runnerAlias;
    }

    public void setRunnerAlias(String runnerAlias) {
        this.runnerAlias = runnerAlias;
    }

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

    public boolean isInstallAsService() {
        return installAsService;
    }

    public void setInstallAsService(boolean installAsService) {
        this.installAsService = installAsService;
    }

    public String getServiceAccountUser() {
        return serviceAccountUser;
    }

    public void setServiceAccountUser(String serviceAccountUser) {
        this.serviceAccountUser = serviceAccountUser;
    }

    public String getServiceAccountPassword() {
        return serviceAccountPassword;
    }

    public void setServiceAccountPassword(String serviceAccountPassword) {
        this.serviceAccountPassword = serviceAccountPassword;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public String getGithubOrganizationUrl() {
        return githubOrganizationUrl;
    }

    public void setGithubOrganizationUrl(String githubOrganizationUrl) {
        this.githubOrganizationUrl = githubOrganizationUrl;
    }

    public String getActionsRunnerDownloadUrl() {
        return actionsRunnerDownloadUrl;
    }

    public void setActionsRunnerDownloadUrl(String actionsRunnerDownloadUrl) {
        this.actionsRunnerDownloadUrl = actionsRunnerDownloadUrl;
    }

    public String getActionsRunnerVersion() {
        return actionsRunnerVersion;
    }

    public void setActionsRunnerVersion(String actionsRunnerVersion) {
        this.actionsRunnerVersion = actionsRunnerVersion;
    }
}
