package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubMemberDTO {
    private String login;
    private Long id;
    private String avatar_url;
    private String url;
    private String name;
    private String company;
    private String type;
    private Boolean site_admin;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public Boolean getSite_admin() {
        return site_admin;
    }

    public void setSite_admin(Boolean site_admin) {
        this.site_admin = site_admin;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
