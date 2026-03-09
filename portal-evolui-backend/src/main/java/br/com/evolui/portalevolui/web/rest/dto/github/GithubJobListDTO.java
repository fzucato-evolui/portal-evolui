package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class GithubJobListDTO {
    private Integer total_count;
    private List<GithubJobDTO> jobs;

    public Integer getTotal_count() {
        return total_count;
    }

    public void setTotal_count(Integer total_count) {
        this.total_count = total_count;
    }

    public List<GithubJobDTO> getJobs() {
        return jobs;
    }

    public void setJobs(List<GithubJobDTO> jobs) {
        this.jobs = jobs;
    }
}
