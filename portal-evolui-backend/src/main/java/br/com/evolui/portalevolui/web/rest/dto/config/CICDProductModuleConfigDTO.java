package br.com.evolui.portalevolui.web.rest.dto.config;

public class CICDProductModuleConfigDTO {
    private Long productId;
    private Boolean enabled;
    private Boolean includeTests;
    private Boolean ignoreHashCommit;

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

    public Boolean getIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(Boolean includeTests) {
        this.includeTests = includeTests;
    }


    public Boolean getIgnoreHashCommit() {
        if (ignoreHashCommit == null) {
            ignoreHashCommit = false;
        }
        return ignoreHashCommit;
    }

    public void setIgnoreHashCommit(Boolean ignoreHashCommit) {
        if (ignoreHashCommit == null) {
            ignoreHashCommit = false;
        }
        this.ignoreHashCommit = ignoreHashCommit;
    }
}
