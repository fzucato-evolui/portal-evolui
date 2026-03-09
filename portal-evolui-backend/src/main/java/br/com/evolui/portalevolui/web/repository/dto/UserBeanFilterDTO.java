package br.com.evolui.portalevolui.web.repository.dto;

import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;

public class UserBeanFilterDTO {
    private Long id;
    private String login;
    private String email;
    private String name;
    private ProfileEnum profile;
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProfileEnum getProfile() {
        return profile;
    }

    public void setProfile(ProfileEnum profile) {
        this.profile = profile;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
