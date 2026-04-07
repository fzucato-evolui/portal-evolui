package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class GithubConfigDTO implements ISystemConfigParser {

    private String user;
    private String token;
    private String owner;
    private Integer daysForKeep;

    /**
     * URL pública (ex.: bucket estático) do zip do client {@code portal-evolui-runner-installer} (Windows + Linux).
     */
    private String runnerInstallerDownloadUrl;

    /**
     * Versão mínima do client exigida pelo portal (semver, ex.: {@code 1.1.0}). O fluxo bloqueia se {@code clientVersion < min}.
     */
    private String runnerInstallerMinVersion;

    @Override
    public GithubConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, GithubConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Integer getDaysForKeep() {
        return daysForKeep;
    }

    public void setDaysForKeep(Integer daysForKeep) {
        this.daysForKeep = daysForKeep;
    }

    public String getRunnerInstallerDownloadUrl() {
        return runnerInstallerDownloadUrl;
    }

    public void setRunnerInstallerDownloadUrl(String runnerInstallerDownloadUrl) {
        this.runnerInstallerDownloadUrl = runnerInstallerDownloadUrl;
    }

    public String getRunnerInstallerMinVersion() {
        return runnerInstallerMinVersion;
    }

    public void setRunnerInstallerMinVersion(String runnerInstallerMinVersion) {
        this.runnerInstallerMinVersion = runnerInstallerMinVersion;
    }
}
