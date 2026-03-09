package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubUserDTO {
    private String name;
    private String email;

    public GithubUserDTO () {

    }
    public GithubUserDTO (String name, String email) {
        this.setName(name);
        this.setEmail(email);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
