package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class GithubWorkflowListDTO {
    private Integer total_count;
    private List<GithubWorkflowDTO> workflow_runs;

    public Integer getTotal_count() {
        return total_count;
    }

    public void setTotal_count(Integer total_count) {
        this.total_count = total_count;
    }

    public List<GithubWorkflowDTO> getWorkflow_runs() {
        return workflow_runs;
    }

    public void setWorkflow_runs(List<GithubWorkflowDTO> workflow_runs) {
        this.workflow_runs = workflow_runs;
    }
}
