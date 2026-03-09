package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Public.class)
public class GeneralConfigDTO implements ISystemConfigParser {

    private String language;

    @Override
    public GeneralConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, GeneralConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
