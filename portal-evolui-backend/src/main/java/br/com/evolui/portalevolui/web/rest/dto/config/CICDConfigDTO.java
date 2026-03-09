package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CICDConfigDTO implements ISystemConfigParser {
    private Boolean enabled;
    private Integer daysForKeep;
    private List<CICDProductConfigDTO> products;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<CICDProductConfigDTO> getProducts() {
        return products;
    }

    public void setProducts(List<CICDProductConfigDTO> products) {
        this.products = products;
    }

    @Override
    public CICDConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CICDConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Integer getDaysForKeep() {
        return daysForKeep;
    }

    public void setDaysForKeep(Integer daysForKeep) {
        this.daysForKeep = daysForKeep;
    }
}
