package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class GithubRunnerDTO {
    private Integer id;
    private String name;
    private String os;
    private String status;
    private boolean busy;
    private List<GithubRunnerLabelDTO> labels;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public List<GithubRunnerLabelDTO> getLabels() {
        return labels;
    }

    public void setLabels(List<GithubRunnerLabelDTO> labels) {
        this.labels = labels;
    }
}
