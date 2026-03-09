package br.com.evolui.portalevolui.web.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class AmbienteModuloConfigDTO {
    private String contextURL;
    private String contextName;
    private String lthUser;
    private String lthPassword;
    private String jvmOptionsCommand;
    private String jvmOptionsLiquibase;
    protected String destinationPath;
    protected String beforeUpdateModuleCommand;
    protected String afterUpdateModuleCommand;
    protected Integer runnerId;
    private String runnerIdentifier;
    private Boolean enabled;
    private List<AmbienteFileMapConfigDTO> filesMap;
    private AmbienteDestinationServerConfigDTO destinationServer;

    public String getContextURL() {
        return contextURL;
    }

    public void setContextURL(String contextURL) {
        this.contextURL = contextURL;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getLthUser() {
        return lthUser;
    }

    public void setLthUser(String lthUser) {
        this.lthUser = lthUser;
    }

    public String getLthPassword() {
        return lthPassword;
    }

    public void setLthPassword(String lthPassword) {
        this.lthPassword = lthPassword;
    }

    public String getJvmOptionsCommand() {
        return jvmOptionsCommand;
    }

    public void setJvmOptionsCommand(String jvmOptionsCommand) {
        this.jvmOptionsCommand = jvmOptionsCommand;
    }

    public String getJvmOptionsLiquibase() {
        return jvmOptionsLiquibase;
    }

    public void setJvmOptionsLiquibase(String jvmOptionsLiquibase) {
        this.jvmOptionsLiquibase = jvmOptionsLiquibase;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getBeforeUpdateModuleCommand() {
        return beforeUpdateModuleCommand;
    }

    public void setBeforeUpdateModuleCommand(String beforeUpdateModuleCommand) {
        this.beforeUpdateModuleCommand = beforeUpdateModuleCommand;
    }

    public String getAfterUpdateModuleCommand() {
        return afterUpdateModuleCommand;
    }

    public void setAfterUpdateModuleCommand(String afterUpdateModuleCommand) {
        this.afterUpdateModuleCommand = afterUpdateModuleCommand;
    }

    public Integer getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(Integer runnerId) {
        this.runnerId = runnerId;
    }

    public String getRunnerIdentifier() {
        return runnerIdentifier;
    }

    public void setRunnerIdentifier(String runnerIdentifier) {
        this.runnerIdentifier = runnerIdentifier;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<AmbienteFileMapConfigDTO> getFilesMap() {
        return filesMap;
    }

    public void setFilesMap(List<AmbienteFileMapConfigDTO> filesMap) {
        this.filesMap = filesMap;
    }

    public void addFileMap(AmbienteFileMapConfigDTO fileMap) {
        if (this.filesMap == null) {
            this.filesMap = new ArrayList<>();
        }
        this.filesMap.add(fileMap);
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
    public static AmbienteModuloConfigDTO fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, AmbienteModuloConfigDTO.class);
        } catch (Exception e) {
            return  null;
        }
    }

    public AmbienteDestinationServerConfigDTO getDestinationServer() {
        return destinationServer;
    }

    public void setDestinationServer(AmbienteDestinationServerConfigDTO destinationServer) {
        this.destinationServer = destinationServer;
    }
}
