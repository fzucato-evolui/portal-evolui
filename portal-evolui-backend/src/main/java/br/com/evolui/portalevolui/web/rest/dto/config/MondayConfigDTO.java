package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class MondayConfigDTO implements ISystemConfigParser {

    private Boolean enabled;
    private String token;
    private String endpoint;
    private String page;
    private MondayVersionGenerationConfigDTO versionGenerationConfig;

    @Override
    public MondayConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, MondayConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public MondayVersionGenerationConfigDTO getVersionGenerationConfig() {
        return versionGenerationConfig;
    }

    public void setVersionGenerationConfig(MondayVersionGenerationConfigDTO versionGenerationConfig) {
        this.versionGenerationConfig = versionGenerationConfig;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
