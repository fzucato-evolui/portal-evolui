package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubAtualizacaoVersaoModuleResultDTO {
    private Boolean enabled;
    private String version;
    private String commit;
    private Boolean executeUpdateCommands;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public Boolean getExecuteUpdateCommands() {
        return executeUpdateCommands;
    }

    public void setExecuteUpdateCommands(Boolean executeUpdateCommands) {
        this.executeUpdateCommands = executeUpdateCommands;
    }
}
