package br.com.evolui.portalevolui.web.json.deserializer;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.config.*;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class SystemConfigBeanDeserializer extends JsonDeserializer<SystemConfigBean> {
    @Override
    public SystemConfigBean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode map = mapper.readTree(jsonParser);

        SystemConfigBean bean = new SystemConfigBean();
        bean.setConfigType(SystemConfigTypeEnum.fromValue(map.get("configType").asText()));
        bean.setId(!map.has("id") ? null : map.get("id").asLong());

        if (bean.getConfigType() == SystemConfigTypeEnum.GOOGLE) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), GoogleConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.GENERAL) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), GeneralConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.AWS) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), AWSConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.GITHUB) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), GithubConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.SMTP) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), SMTPConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.NOTIFICATION) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), NotificationConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.MONDAY) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), MondayConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.CICD) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), CICDConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.AX) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), AXConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.PORTAL_LUTHIER) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), PortalLuthierConfigDTO.class));
        } else if (bean.getConfigType() == SystemConfigTypeEnum.HEALTH_CHECKER) {
            bean.setConfig(mapper.readValue(map.get("config").toString(), HealthCheckerConfigDTO.class));
        }
        return bean;
    }
}
