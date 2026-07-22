package br.com.evolui.portalevolui.web.beans.dto;

public class AmbienteFileMapConfigDTO {
    private String source;
    private String destination;
    private String beforeUpdateCommand;
    private String afterUpdateCommand;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getBeforeUpdateCommand() {
        return beforeUpdateCommand;
    }

    public void setBeforeUpdateCommand(String beforeUpdateCommand) {
        this.beforeUpdateCommand = beforeUpdateCommand;
    }

    public String getAfterUpdateCommand() {
        return afterUpdateCommand;
    }

    public void setAfterUpdateCommand(String afterUpdateCommand) {
        this.afterUpdateCommand = afterUpdateCommand;
    }
}
