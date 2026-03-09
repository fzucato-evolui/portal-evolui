package br.com.evolui.portalevolui.shared.dto;

import java.util.ArrayList;
import java.util.List;

public class HealthCheckerDTO {
    private Long id;
    private List<HealthCheckerModuleDTO> modules;
    private List<HealthCheckerAlertDTO> alerts;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<HealthCheckerModuleDTO> getModules() {
        return modules;
    }

    public void setModules(List<HealthCheckerModuleDTO> modules) {
        this.modules = modules;
    }

    public void addModule(HealthCheckerModuleDTO module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();
        }
        this.modules.add(module);
    }

    public List<HealthCheckerAlertDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<HealthCheckerAlertDTO> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(HealthCheckerAlertDTO alert) {
        if (this.alerts == null) {
            this.alerts = new ArrayList<>();
        }
        this.alerts.add(alert);
    }
}
