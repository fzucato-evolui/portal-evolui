package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubBranchCreateResponseDTO {
    private String ref;
    private String nodeId;
    private String url;
    private GitObjectDTO object;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public GitObjectDTO getObject() {
        return object;
    }

    public void setObject(GitObjectDTO object) {
        this.object = object;
    }
}
