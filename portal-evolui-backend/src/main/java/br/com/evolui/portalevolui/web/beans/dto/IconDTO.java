package br.com.evolui.portalevolui.web.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IconDTO {
    private String fontSet;
    private String fontIcon;

    public String getFontSet() {
        return fontSet;
    }

    public void setFontSet(String fontSet) {
        this.fontSet = fontSet;
    }

    public String getFontIcon() {
        return fontIcon;
    }

    public void setFontIcon(String fontIcon) {
        this.fontIcon = fontIcon;
    }

    @JsonIgnore
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return  null;
        }
    }

    @JsonIgnore
    public static IconDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, IconDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }
}
