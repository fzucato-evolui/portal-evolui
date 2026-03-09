package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.ArrayList;
import java.util.List;

public class GithubDiffCommitListDTO {
    private Integer total_commits;
    private Integer ahead_by;
    private Integer behind_by;
    private List<GithubCommitDTO> commits;
    private List<GithubDiffCommitFileDTO> files;

    public Integer getTotal_commits() {
        return total_commits;
    }

    public void setTotal_commits(Integer total_commits) {
        this.total_commits = total_commits;
    }

    public Integer getAhead_by() {
        return ahead_by;
    }

    public void setAhead_by(Integer ahead_by) {
        this.ahead_by = ahead_by;
    }

    public Integer getBehind_by() {
        return behind_by;
    }

    public void setBehind_by(Integer behind_by) {
        this.behind_by = behind_by;
    }

    public List<GithubCommitDTO> getCommits() {
        return commits;
    }

    public void setCommits(List<GithubCommitDTO> commits) {
        this.commits = commits;
    }

    public List<GithubDiffCommitFileDTO> getFiles() {
        if (files == null) {
            this.files = new ArrayList<>();
        }
        return files;
    }

    public void setFiles(List<GithubDiffCommitFileDTO> files) {
        this.files = files;
    }
}
