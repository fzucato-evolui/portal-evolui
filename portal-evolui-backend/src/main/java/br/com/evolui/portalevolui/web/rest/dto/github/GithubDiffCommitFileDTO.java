package br.com.evolui.portalevolui.web.rest.dto.github;

public class GithubDiffCommitFileDTO {
    private String sha;
    private String filename;
    private String status;
    private String blob_url;
    private String raw_url;
    private String contents_url;
    private String patch;


    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBlob_url() {
        return blob_url;
    }

    public void setBlob_url(String blob_url) {
        this.blob_url = blob_url;
    }

    public String getRaw_url() {
        return raw_url;
    }

    public void setRaw_url(String raw_url) {
        this.raw_url = raw_url;
    }

    public String getContents_url() {
        return contents_url;
    }

    public void setContents_url(String contents_url) {
        this.contents_url = contents_url;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }
}
