package br.com.evolui.portalevolui.shared.dto;

/**
 * Versão do client abaixo do mínimo configurado; enviado para browser e Go em {@code /queue/runner-install-blocked/...}.
 */
public class RunnerInstallerBlockedDTO {

    private String reason;
    private String minVersionRequired;
    private String clientVersion;
    private String installerDownloadUrl;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMinVersionRequired() {
        return minVersionRequired;
    }

    public void setMinVersionRequired(String minVersionRequired) {
        this.minVersionRequired = minVersionRequired;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getInstallerDownloadUrl() {
        return installerDownloadUrl;
    }

    public void setInstallerDownloadUrl(String installerDownloadUrl) {
        this.installerDownloadUrl = installerDownloadUrl;
    }
}
