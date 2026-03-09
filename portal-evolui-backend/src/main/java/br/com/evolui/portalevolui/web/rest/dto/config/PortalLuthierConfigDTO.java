package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class PortalLuthierConfigDTO implements ISystemConfigParser {
    private Boolean enabled;
    private String server;
    private String user;
    private String password;
    private String luthierUser;
    private String luthierPassword;
    @Override
    public PortalLuthierConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, PortalLuthierConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLuthierUser() {
        return luthierUser;
    }

    public void setLuthierUser(String luthierUser) {
        this.luthierUser = luthierUser;
    }

    public String getLuthierPassword() {
        return luthierPassword;
    }

    public void setLuthierPassword(String luthierPassword) {
        this.luthierPassword = luthierPassword;
    }
}
