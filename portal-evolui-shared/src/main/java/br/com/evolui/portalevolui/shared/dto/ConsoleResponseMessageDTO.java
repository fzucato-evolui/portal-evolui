package br.com.evolui.portalevolui.shared.dto;

public class ConsoleResponseMessageDTO {
    private String currentDirectory;
    private boolean finished;
    private String output;
    private String outputError;
    private Integer sequence;


    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getOutputError() {
        return outputError;
    }

    public void setOutputError(String outputError) {
        this.outputError = outputError;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
