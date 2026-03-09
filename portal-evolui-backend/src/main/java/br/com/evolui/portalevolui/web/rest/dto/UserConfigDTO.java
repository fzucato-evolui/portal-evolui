package br.com.evolui.portalevolui.web.rest.dto;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonView;

@JsonView(JsonViewerPattern.Public.class)
public class UserConfigDTO {
    private String lang;
    private String theme;
    private String scheme;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}
