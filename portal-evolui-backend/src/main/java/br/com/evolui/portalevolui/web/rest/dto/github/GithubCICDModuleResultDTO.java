package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.enums.CICDReportStatusTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GithubCICDModuleResultDTO {
    private Boolean enabled;
    private Boolean includeTests;
    private String tag;
    private String branch;
    private String commit;
    private String repository;
    private String relativePath;
    private Summary buildSumary;
    private Summary testSumary;
    private Boolean fatalError;
    private Long checkRunId;
    private CICDReportStatusTypeEnum status;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Summary getBuildSumary() {
        return buildSumary;
    }

    public void setBuildSumary(Summary buildSumary) {
        this.buildSumary = buildSumary;
    }

    public Summary getTestSumary() {
        return testSumary;
    }

    public void setTestSumary(Summary testSumary) {
        this.testSumary = testSumary;
    }

    public Boolean getFatalError() {
        return fatalError;
    }

    public void setFatalError(Boolean fatalError) {
        this.fatalError = fatalError;
    }

    public Long getCheckRunId() {
        return checkRunId;
    }

    public void setCheckRunId(Long checkRunId) {
        this.checkRunId = checkRunId;
    }

    public CICDReportStatusTypeEnum getStatus() {
        return status;
    }

    public void setStatus(CICDReportStatusTypeEnum status) {
        this.status = status;
    }

    public Boolean getIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(Boolean includeTests) {
        this.includeTests = includeTests;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public static class Summary {
        private Integer skipped;
        private Integer success;
        private Integer failure;
        private Double totalTime;

        public Integer getSkipped() {
            return skipped;
        }

        public void setSkipped(Integer skipped) {
            this.skipped = skipped;
        }

        public Integer getSuccess() {
            return success;
        }

        public void setSuccess(Integer success) {
            this.success = success;
        }

        public Integer getFailure() {
            return failure;
        }

        public void setFailure(Integer failure) {
            this.failure = failure;
        }

        public Double getTotalTime() {
            return totalTime;
        }

        public void setTotalTime(Double totalTime) {
            this.totalTime = totalTime;
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
        public static Summary fromJson(String json) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return new ObjectMapper().readValue(json, Summary.class);
            } catch (Exception e) {
                return  null;
            }
        }
    }
}
