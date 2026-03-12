package br.com.evolui.portalevolui.web.rest.dto.config;

import java.util.List;

public class CICDProjectConfigDTO {
    private Long productId;
    private String compileType;
    private String branch;
    private Boolean enabled;

    private String cronExpression;
    private List<CICDProjectModuleConfigDTO> modules;

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

    public List<CICDProjectModuleConfigDTO> getModules() {
        return modules;
    }

    public void setModules(List<CICDProjectModuleConfigDTO> modules) {
        this.modules = modules;
    }


    public String getCompileType() {
        return compileType;
    }

    public void setCompileType(String compileType) {
        this.compileType = compileType;
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
