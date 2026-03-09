package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.Calendar;
import java.util.List;

public class GithubDetailedRepositoryDTO {
    private Long id;
    private String node_id;
    private String name;
    private String full_name;
    private String html_url;
    private String description;
    private String url;
    private Calendar created_at;
    private Calendar updated_at;
    private Calendar pushed_at;

    private String language;
    private boolean archived;
    private boolean disabled;
    
    private String default_branch;
    private List<String> topics;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getHtml_url() {
        return html_url;
    }

    public void setHtml_url(String html_url) {
        this.html_url = html_url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Calendar getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Calendar created_at) {
        this.created_at = created_at;
    }

    public Calendar getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Calendar updated_at) {
        this.updated_at = updated_at;
    }

    public Calendar getPushed_at() {
        return pushed_at;
    }

    public void setPushed_at(Calendar pushed_at) {
        this.pushed_at = pushed_at;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getDefault_branch() {
        return default_branch;
    }

    public void setDefault_branch(String default_branch) {
        this.default_branch = default_branch;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }
}
