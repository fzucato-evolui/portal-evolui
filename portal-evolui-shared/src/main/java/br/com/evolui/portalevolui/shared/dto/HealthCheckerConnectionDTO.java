package br.com.evolui.portalevolui.shared.dto;

public class HealthCheckerConnectionDTO {
    private String destination;
    private LoginDTO login;
    private String host;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LoginDTO getLogin() {
        return login;
    }

    public void setLogin(LoginDTO login) {
        this.login = login;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
