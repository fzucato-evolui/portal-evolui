package br.com.evolui.portalevolui.web.rest.dto.github.runner;

/**
 * Resposta do client às perguntas sobre a máquina (corpo STOMP); campos preenchidos pelo Go.
 */
public class RunnerInstallMachineInfoResponseDTO {

    private String osFamily;
    private String arch;
    private String hostname;
    private boolean meetsMinimumRequirements;
    private String requirementsDetail;

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

    public boolean isMeetsMinimumRequirements() {
        return meetsMinimumRequirements;
    }

    public void setMeetsMinimumRequirements(boolean meetsMinimumRequirements) {
        this.meetsMinimumRequirements = meetsMinimumRequirements;
    }

    public String getRequirementsDetail() {
        return requirementsDetail;
    }

    public void setRequirementsDetail(String requirementsDetail) {
        this.requirementsDetail = requirementsDetail;
    }
}
