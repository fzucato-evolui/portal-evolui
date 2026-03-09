package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.enums.HealthCheckerModuleTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HealthCheckerModuleConfigDTO {
    private Long id;
    private HealthCheckerModuleTypeEnum moduleType;
    private String identifier;
    private String description;
    private String commandAddress;
    private String acceptableResponsePattern;
    private CertificateDTO clientCertificate;
    private boolean bypassCertificate = true;
    private boolean sendNotification;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HealthCheckerModuleTypeEnum getModuleType() {
        return moduleType;
    }

    public void setModuleType(HealthCheckerModuleTypeEnum moduleType) {
        this.moduleType = moduleType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommandAddress() {
        return commandAddress;
    }

    public void setCommandAddress(String commandAddress) {
        this.commandAddress = commandAddress;
    }

    public String getAcceptableResponsePattern() {
        return acceptableResponsePattern;
    }

    public void setAcceptableResponsePattern(String acceptableResponsePattern) {
        this.acceptableResponsePattern = acceptableResponsePattern;
    }

    public CertificateDTO getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(CertificateDTO clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public boolean isBypassCertificate() {
        return bypassCertificate;
    }

    public void setBypassCertificate(boolean bypassCertificate) {
        this.bypassCertificate = bypassCertificate;
    }

    public boolean isSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
    @JsonIgnore
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return  null;
        }
    }

    @JsonIgnore
    public static HealthCheckerModuleConfigDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, HealthCheckerModuleConfigDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }

}
