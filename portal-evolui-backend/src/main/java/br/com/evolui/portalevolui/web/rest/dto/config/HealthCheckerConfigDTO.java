package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class HealthCheckerConfigDTO implements ISystemConfigParser {

    /**
     * URL pública (ex.: bucket estático) do zip com binários Windows/Linux do Evolui Monitor ({@code portal-evolui-monitor}).
     */
    private String monitorDownloadUrl;

    /**
     * Versão mínima do agente exigida pelo portal (semver, ex.: {@code 1.0.0}). Verificação na conexão pode ser implementada depois.
     */
    private String monitorMinVersion;

    @Override
    public HealthCheckerConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, HealthCheckerConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getMonitorDownloadUrl() {
        return monitorDownloadUrl;
    }

    public void setMonitorDownloadUrl(String monitorDownloadUrl) {
        this.monitorDownloadUrl = monitorDownloadUrl;
    }

    public String getMonitorMinVersion() {
        return monitorMinVersion;
    }

    public void setMonitorMinVersion(String monitorMinVersion) {
        this.monitorMinVersion = monitorMinVersion;
    }
}
