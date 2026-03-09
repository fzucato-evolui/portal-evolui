package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonView(JsonViewerPattern.Public.class)
public class HealthCheckerSimpleSystemInfoDTO {
    private String memory;
    private String machine;
    private String software;
    private String disks;
    private String processor;

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getDisks() {
        return disks;
    }

    public void setDisks(String disks) {
        this.disks = disks;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
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
    public static HealthCheckerSimpleSystemInfoDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, HealthCheckerSimpleSystemInfoDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }
}
