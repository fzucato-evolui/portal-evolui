package br.com.evolui.portalevolui.web.rest.dto.config;

import java.util.List;

public class CICDProductConfigDTO {
    private Long productId;
    private String branch;
    private Boolean enabled;

    private String cronExpression;
    private List<CICDProductModuleConfigDTO> modules;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<CICDProductModuleConfigDTO> getModules() {
        return modules;
    }

    public void setModules(List<CICDProductModuleConfigDTO> modules) {
        this.modules = modules;
    }


    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
