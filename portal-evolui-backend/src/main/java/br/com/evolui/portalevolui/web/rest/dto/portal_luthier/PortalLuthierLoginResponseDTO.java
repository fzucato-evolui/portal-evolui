package br.com.evolui.portalevolui.web.rest.dto.portal_luthier;

public class PortalLuthierLoginResponseDTO {
    private String accessToken;
    private PortalLuthierUserDTO user;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public PortalLuthierUserDTO getUser() {
        return user;
    }

    public void setUser(PortalLuthierUserDTO user) {
        this.user = user;
    }
}
