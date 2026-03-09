package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.enums.HealthCheckerAlertTypeEnum;

public class HealthCheckerModuleDTO extends HealthCheckerAlertDTO {
    private Long id;

    public HealthCheckerModuleDTO() {
        this.alertType = HealthCheckerAlertTypeEnum.MODULE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
