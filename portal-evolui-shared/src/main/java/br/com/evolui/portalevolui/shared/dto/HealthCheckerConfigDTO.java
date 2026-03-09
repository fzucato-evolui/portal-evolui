package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.enums.HealthCheckerAlertTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class HealthCheckerConfigDTO {
    private Long id;
    private String host;
    private String identifier;
    private String description;
    private Integer healthCheckInterval;
    private List<HealthCheckerModuleConfigDTO> modules;
    private LoginDTO login;
    private LinkedHashMap<HealthCheckerAlertTypeEnum, HealthCheckerAlertConfigDTO> alerts;
    private HealthCheckerSimpleSystemInfoDTO systemInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public List<HealthCheckerModuleConfigDTO> getModules() {
        return modules;
    }

    public void setModules(List<HealthCheckerModuleConfigDTO> modules) {
        this.modules = modules;
    }

    public void addModule(HealthCheckerModuleConfigDTO module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();
        }
        this.modules.add(module);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public LoginDTO getLogin() {
        return login;
    }

    public void setLogin(LoginDTO login) {
        this.login = login;
    }

    public LinkedHashMap<HealthCheckerAlertTypeEnum, HealthCheckerAlertConfigDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(LinkedHashMap<HealthCheckerAlertTypeEnum, HealthCheckerAlertConfigDTO> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(HealthCheckerAlertTypeEnum alertType, HealthCheckerAlertConfigDTO config) {
        if (this.alerts == null) {
            this.alerts = new LinkedHashMap<>();
        }
        this.alerts.put(alertType, config);
    }

    public HealthCheckerSimpleSystemInfoDTO getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(HealthCheckerSimpleSystemInfoDTO systemInfo) {
        this.systemInfo = systemInfo;
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
    public static HealthCheckerConfigDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, HealthCheckerConfigDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }
}
