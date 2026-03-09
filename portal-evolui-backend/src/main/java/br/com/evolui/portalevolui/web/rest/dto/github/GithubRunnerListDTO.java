package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class GithubRunnerListDTO {
    private Integer total_count;
    private List<GithubRunnerDTO> runners;

    public Integer getTotal_count() {
        return total_count;
    }

    public void setTotal_count(Integer total_count) {
        this.total_count = total_count;
    }

    public List<GithubRunnerDTO> getRunners() {
        return runners;
    }

    public void setRunners(List<GithubRunnerDTO> runners) {
        this.runners = runners;
    }
}
