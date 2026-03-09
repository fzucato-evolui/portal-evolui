package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GoogleConfigDTO implements ISystemConfigParser {

    @JsonView(JsonViewerPattern.Admin.class)
    private String apiKey;

    @JsonView(JsonViewerPattern.Public.class)
    private String clientID;

    @JsonView(JsonViewerPattern.Admin.class)
    private String secretKey;

    @JsonView(JsonViewerPattern.Admin.class)
    private GoogleServiceAccountConfigDTO serviceAccount;

    @Override
    public GoogleConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, GoogleConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public GoogleServiceAccountConfigDTO getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(GoogleServiceAccountConfigDTO serviceAccount) {
        this.serviceAccount = serviceAccount;
    }
}
