package br.com.evolui.portalevolui.web.json.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LinkedHashMapDeserializer extends JsonDeserializer<LinkedHashMap> {
    @Override
    public LinkedHashMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode map = mapper.readTree(jsonParser);
        if (map != null && StringUtils.hasText(map.toString())) {
            try {
                return mapper.readValue(map.toString(), LinkedHashMap.class);
            } catch (Exception ex) {
                List<Map.Entry<Object, Object>> entries = mapper.readValue(map.toString(), new TypeReference<>() {
                });
                LinkedHashMap ret = new LinkedHashMap();
                for (Map.Entry e : entries) {
                    ret.put(e.getKey(), e.getValue());
                }
                return ret;
            }
        }
        return null;

    }
}
