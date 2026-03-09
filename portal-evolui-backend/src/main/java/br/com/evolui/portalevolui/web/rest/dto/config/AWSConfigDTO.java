package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigParser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JsonView(JsonViewerPattern.Admin.class)
public class AWSConfigDTO implements ISystemConfigParser {
    private Integer daysForKeep;
    private LinkedHashMap<String, AWSAccountConfigDTO> accountConfigs = new LinkedHashMap<>();

    @Override
    public AWSConfigDTO parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, AWSConfigDTO.class);
    }

    @Override
    public String getJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public LinkedHashMap<String, AWSAccountConfigDTO> getAccountConfigs() {
        return accountConfigs;
    }

    public void setAccountConfigs(LinkedHashMap<String, AWSAccountConfigDTO> accountConfigs) {
        this.accountConfigs = accountConfigs;
    }

    public void putConfig(String type, AWSAccountConfigDTO config) {
        if (this.accountConfigs == null) {
            this.accountConfigs = new LinkedHashMap<>();
        }
        this.accountConfigs.put(type, config);
    }

    @JsonIgnore
    public Map.Entry<String, AWSAccountConfigDTO> getMainAccount() {
        if (this.accountConfigs == null) {
            return null;
        }
        return this.accountConfigs.entrySet().stream().filter(x -> x.getValue().getMain() != null && x.getValue().getMain().booleanValue()).findFirst().orElse(this.accountConfigs.entrySet().stream().findFirst().orElse(null));
    }

    @JsonIgnore
    public LinkedHashMap<String, AWSAccountConfigDTO> getEnabledAccounts() {
        if (this.accountConfigs == null) {
            return null;
        }
        return this.accountConfigs.entrySet().stream()
                .filter(x -> x.getValue().getEnabled() != null && x.getValue().getEnabled().booleanValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public Integer getDaysForKeep() {
        return daysForKeep;
    }

    public void setDaysForKeep(Integer daysForKeep) {
        this.daysForKeep = daysForKeep;
    }
}
