package br.com.evolui.portalevolui.web.rest.dto.monday;

import br.com.evolui.portalevolui.web.rest.dto.enums.MondayColumnTypesEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;

public class MondayColumnDTO {
    private String id;
    private String text;
    private MondayColumnTypesEnum type;
    private  String title;
    private String settings_str;
    private LinkedHashMap<String, String> possibleValues;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MondayColumnTypesEnum getType() {
        return type;
    }

    public void setType(MondayColumnTypesEnum type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSettings_str() {
        return settings_str;
    }

    public void setSettings_str(String settings_str) {
        this.settings_str = settings_str;
        this.getPossibleValues();
    }

    public LinkedHashMap<String, String> getPossibleValues() {
        if (this.possibleValues == null && StringUtils.hasText(this.settings_str)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                LinkedHashMap<Object, Object> allValues = mapper.readValue(this.settings_str, new TypeReference<LinkedHashMap<Object, Object>>() { });
                this.possibleValues = (LinkedHashMap<String, String>) allValues.get("labels");
            } catch (Exception e) {

            }
        }
        return possibleValues;
    }
}
