package br.com.evolui.portalevolui.shared.dto;

/**
 * Primeira mensagem do client Go após conectar (corpo de {@link WebSocketMessageDTO}).
 */
public class RunnerInstallerHelloDTO {

    /** Versão semver do binário (ex.: 0.1.0). */
    private String clientVersion;

    /** Ex.: windows, linux. */
    private String osFamily;

    private String arch;

    private String hostname;

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getOsFamily() {
        return osFamily;
    }

    public void setOsFamily(String osFamily) {
        this.osFamily = osFamily;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
