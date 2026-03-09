package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@JsonView(JsonViewerPattern.Admin.class)
//@JsonDeserialize(using = NotificationConfigDTODeserializer.class)
public class NotificationConfigDTO implements ISystemConfigParser {
    private List<NotificationTriggerConfigDTO> configs = new ArrayList();

    public List<NotificationTriggerConfigDTO> getConfigs() {
        return configs;
    }

    public void setConfigs(List<NotificationTriggerConfigDTO> configs) {
        this.configs = configs;
    }

    public void addConfig(NotificationTriggerConfigDTO config) {
        if (this.configs == null) {
            this.configs = new ArrayList<>();
        }
        this.configs.add(config);
    }

    @Override
    public NotificationConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, NotificationConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

}
