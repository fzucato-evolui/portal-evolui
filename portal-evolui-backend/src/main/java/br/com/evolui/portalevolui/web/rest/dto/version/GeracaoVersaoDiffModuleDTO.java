package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.ProjectModuleBean;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubDiffCommitListDTO;

public class GeracaoVersaoDiffModuleDTO {
    private ProjectModuleBean module;
    private GithubDiffCommitListDTO diffs;

    public ProjectModuleBean getModule() {
        return module;
    }

    public void setModule(ProjectModuleBean module) {
        this.module = module;
    }

    public GithubDiffCommitListDTO getDiffs() {
        return diffs;
    }

    public void setDiffs(GithubDiffCommitListDTO diffs) {
        this.diffs = diffs;
    }
}
