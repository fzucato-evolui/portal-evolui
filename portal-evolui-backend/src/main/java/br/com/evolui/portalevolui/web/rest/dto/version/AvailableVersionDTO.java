package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.VersaoBuildBaseBean;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubBranchDTO;

import java.util.ArrayList;
import java.util.List;

public class AvailableVersionDTO {
    private List<BranchDTO> branches;

    public List<BranchDTO> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchDTO> branches) {
        this.branches = branches;
    }

    public void addBranch(BranchDTO branch) {
        if (this.branches == null) {
            this.branches = new ArrayList<>();
        }
        this.branches.add(branch);
    }

    public static AvailableVersionDTO parseFromGithubBranches(List<GithubBranchDTO> branches) {
        AvailableVersionDTO dto = new AvailableVersionDTO();
        for(GithubBranchDTO b : branches) {
            if (b.getName().startsWith("tags/")) {
                continue;
            }
            BranchDTO branchDTO = new BranchDTO();
            branchDTO.setVersion(b.getName());
            dto.addBranch(branchDTO);
        }
        for(GithubBranchDTO b : branches) {
            if (!b.getName().startsWith("tags/")) {
                continue;
            }
            VersaoBuildBaseBean build = new VersaoBuildBaseBean(b.getName().replace("tags/", ""));

            BranchDTO branchDTO = dto.branches.stream().filter(x -> x.getVersion().equals(build.getBranch())).findFirst().orElse(null);
            if(branchDTO != null) {
                branchDTO.addTag(build.getTag());
            }
        }
        return dto;
    }
}
