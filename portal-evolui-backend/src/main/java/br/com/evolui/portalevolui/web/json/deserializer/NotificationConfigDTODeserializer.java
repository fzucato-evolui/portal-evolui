package br.com.evolui.portalevolui.web.json.deserializer;

import br.com.evolui.portalevolui.web.rest.dto.config.NotificationConfigDTO;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;

public class NotificationConfigDTODeserializer extends JsonDeserializer<NotificationConfigDTO> {
    @Override
    public NotificationConfigDTO deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode map = mapper.readTree(jsonParser);

        NotificationConfigDTO dto = new NotificationConfigDTO();
        LinkedHashMap<String, Object> configs = mapper.readValue(map.get("configs").toString(), LinkedHashMap.class);

        /*
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            NotificationTypeEnum key = NotificationTypeEnum.fromValue(entry.getKey());
            if (key == NotificationTypeEnum.EMAIL) {
                dto.putConfig(key, mapper.readValue(mapper.writeValueAsString(entry.getValue()), NotificationEmailConfigDTO.class));
            } else if (key == NotificationTypeEnum.WHATSAPP) {
                dto.putConfig(key, mapper.readValue(mapper.writeValueAsString(entry.getValue()), NotificationWhatsappConfigDTO.class));
            }

        }
        */
        return dto;
    }
}
