package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.List;

public class BranchesAndTagsDetailDTO {
    private List<GitRefDetailDTO> branches;
    private List<GitRefDetailDTO> tags;

    public BranchesAndTagsDetailDTO() {}

    public BranchesAndTagsDetailDTO(List<GitRefDetailDTO> branches, List<GitRefDetailDTO> tags) {
        this.branches = branches;
        this.tags = tags;
    }

    public List<GitRefDetailDTO> getBranches() {
        return branches;
    }

    public void setBranches(List<GitRefDetailDTO> branches) {
        this.branches = branches;
    }

    public List<GitRefDetailDTO> getTags() {
        return tags;
    }

    public void setTags(List<GitRefDetailDTO> tags) {
        this.tags = tags;
    }
}
