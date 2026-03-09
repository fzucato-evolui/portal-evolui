package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.util.GeradorTokenPortalEvolui;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginDTO <T> {
    private String login;
    private String password;
    private T extraInfo;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonIgnore
    public String getToken(String key) throws Exception {
        return GeradorTokenPortalEvolui.generateToken(key, getJson());
    }

    public static LoginDTO getFromToken(String key, String token) throws Exception {
        String json = GeradorTokenPortalEvolui.getFromToken(token, key);
        return new ObjectMapper().readValue(json, LoginDTO.class);
    }

    private String getJson() throws Exception {
        return new ObjectMapper().writeValueAsString(this);
    }

    public T getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(T extraInfo) {
        this.extraInfo = extraInfo;
    }
}
