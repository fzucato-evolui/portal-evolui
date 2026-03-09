package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.VersaoBuildBaseBean;
import org.hibernate.internal.util.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class BranchDTO {
    private String version;
    private String lastTag;
    private List<String> tags;
    private boolean abnormalBranch;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        VersaoBuildBaseBean bean = new VersaoBuildBaseBean(version);
        this.version = bean.getBranch();
        this.setAbnormalBranch(bean.isAbnormalBranch());
    }

    public String getLastTag() {
        return lastTag;
    }

    public void setLastTag(String lastTag) {
        this.lastTag = lastTag;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
        if (StringHelper.isEmpty(this.lastTag)) {
            this.lastTag = tag;
        } else {
            if (new VersaoBuildBaseBean(tag).compareTo(new VersaoBuildBaseBean(this.lastTag)) > 0) {
                this.lastTag = tag;
            }
        }
    }

    public boolean isAbnormalBranch() {
        return abnormalBranch;
    }

    public void setAbnormalBranch(boolean abnormalBranch) {
        this.abnormalBranch = abnormalBranch;
    }
}
