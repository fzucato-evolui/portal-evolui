package br.com.evolui.portalevolui.shared.dto;

/**
 * Conteúdo JSON criptografado no token exibido para o {@code portal-evolui-runner-installer} (via {@code GeradorTokenPortalEvolui}).
 */
public class RunnerInstallerConnectionDTO {

    /** Identificador da sessão modal; usado no header {@code Identifier} WebSocket e sufixo das filas do client Go. */
    private String uuid;

    /** Host do portal (ex.: https://host:porta). */
    private String host;

    /**
     * JWT do usuário logado no momento da geração do token — usado só no handshake WebSocket do client Go
     * (query {@code Authorization}, autenticação Spring). As filas STOMP desta sessão usam {@link #getUuid()} no browser
     * e no Go ({@code /queue/.../}&lt;uuid&gt;), para não colidir com outras abas do mesmo usuário.
     */
    private String destination;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
