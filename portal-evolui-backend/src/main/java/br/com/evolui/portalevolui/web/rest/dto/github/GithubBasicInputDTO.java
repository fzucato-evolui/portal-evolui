package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubBasicInputDTO extends GithubVersaoDTO {
    private String hashToken;
    private String webhook;
    private GithubUserDTO user;

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }

    public GithubUserDTO getUser() {
        return user;
    }

    public void setUser(GithubUserDTO user) {
        this.user = user;
    }
}
