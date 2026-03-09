package br.com.evolui.portalevolui.web.rest.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubContentDTO {

    private String name;
    private String path;
    private String sha;
    private Long size;
    private String type;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("download_url")
    private String downloadUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isFile() {
        return "file".equals(type);
    }

    public boolean isDirectory() {
        return "dir".equals(type);
    }
}
