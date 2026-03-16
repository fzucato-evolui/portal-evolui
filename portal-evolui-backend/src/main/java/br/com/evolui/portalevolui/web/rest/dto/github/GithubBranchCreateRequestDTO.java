package br.com.evolui.portalevolui.web.rest.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.StringUtils;

public class GithubBranchCreateRequestDTO {
    @JsonIgnore
    private String branchName;
    private String ref;
    private String sha;

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getRef() {
        if (StringUtils.hasText(branchName)) {
            return "refs/heads/" + branchName;
        }
        return null;
    }
}
