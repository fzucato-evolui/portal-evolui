package br.com.evolui.portalevolui.web.rest.dto.portal_luthier;

import java.util.LinkedHashSet;

public class PortalLuthierDatabaseDTO {
    private Long id;
    private String identifier;
    private String description;
    private String host;
    private String databaseType;
    private String user;
    private String password;
    private String database;
    private String repository;
    private String branch;
    private String hashCommit;
    private String metadataFolderPath;
    private LinkedHashSet<String> clientKeywords;
    private boolean ableToAudit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getHashCommit() {
        return hashCommit;
    }

    public void setHashCommit(String hashCommit) {
        this.hashCommit = hashCommit;
    }

    public String getMetadataFolderPath() {
        return metadataFolderPath;
    }

    public void setMetadataFolderPath(String metadataFolderPath) {
        this.metadataFolderPath = metadataFolderPath;
    }

    public LinkedHashSet<String> getClientKeywords() {
        return clientKeywords;
    }

    public void setClientKeywords(LinkedHashSet<String> clientKeywords) {
        this.clientKeywords = clientKeywords;
    }

    public boolean isAbleToAudit() {
        return ableToAudit;
    }

    public void setAbleToAudit(boolean ableToAudit) {
        this.ableToAudit = ableToAudit;
    }
}

