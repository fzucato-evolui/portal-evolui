package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.TimeZone;

public class GithubCICDResultDTO {
    private Long id;
    private GithubActionConclusionEnum conclusion;
    private GithubActionStatusEnum status;
    private Calendar conclusionDate;
    private Result result;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GithubActionConclusionEnum getConclusion() {
        return conclusion;
    }

    public void setConclusion(GithubActionConclusionEnum conclusion) {
        this.conclusion = conclusion;
    }
    public GithubActionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GithubActionStatusEnum status) {
        this.status = status;
    }

    public Calendar getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(Calendar conclusionDate) {
        conclusionDate.setTimeZone(TimeZone.getDefault());
        this.conclusionDate = conclusionDate;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public class Result {
        private LinkedHashMap<String, GithubCICDModuleResultDTO> modules;
        private Long workflowId;
        private GithubRepositoryDTO repository;
        private String tag;
        private String branch;
        public Long getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(Long workflowId) {
            this.workflowId = workflowId;
        }

        public GithubRepositoryDTO getRepository() {
            return repository;
        }

        public void setRepository(GithubRepositoryDTO repository) {
            this.repository = repository;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public LinkedHashMap<String, GithubCICDModuleResultDTO> getModules() {
            return modules;
        }

        public void setModules(LinkedHashMap<String, GithubCICDModuleResultDTO> modules) {
            this.modules = modules;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
    }
}
