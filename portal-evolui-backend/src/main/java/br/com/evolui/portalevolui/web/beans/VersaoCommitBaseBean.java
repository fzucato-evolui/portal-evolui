package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@JsonView(JsonViewerPattern.Admin.class)
public class VersaoCommitBaseBean extends VersaoBuildBaseBean{
    @Column(name = "hash_commit", length = 100)
    private String commit;

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }
}
