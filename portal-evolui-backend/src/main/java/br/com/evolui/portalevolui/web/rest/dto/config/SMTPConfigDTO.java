package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Admin.class)
public class SMTPConfigDTO implements ISystemConfigParser {
    private String server;
    private Integer port;
    private Boolean ssl;
    private String user;
    private String password;
    private String senderName;
    private String senderEmail;
    @Override
    public SMTPConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, SMTPConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
