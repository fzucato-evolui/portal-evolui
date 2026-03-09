package br.com.evolui.portalevolui.web.rest.dto.portal_luthier;

import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.enums.DatabaseTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class PortalLuthierContextDTO {

    private Long id;
    
    private String context;
    
    private Integer databaseMaxpool;
    
    private Integer databaseMinpool;
    
    private Integer metadataMaxpool;
    
    private Integer metadataMinpool;
    
    private String serverUrl;

    private String description;

    private String configuration;

    private Integer debugDataBase = -1;

    private String licenseServer;

    private String extraConfiguration;

    private String dbExtra;

    private String typeAuthentication;

    private String xmlAuthentication = "<auth>\n\t<class>Basic</class>\n\t<conf/>\n</auth>";

    private String metadataFile;

    private String luthierProviderService;

    private Boolean disableLibs;

    private List<PortalLuthierServiceProviderDTO> serviceProvidersDataCollection;

    private String typeDataBase;

    private String dataBase;

    private String server;

    private String userDataBase;

    private String passDataBase;

    private Boolean hasStorageProvider;

    private String repository;

    private String branch;

    private String hashCommit;

    private String metadataFolderPath;

    private boolean ableToAudit;

    // Não fazem parte da resposta
    private String luthierUser;
    private String luthierPassword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getDatabaseMaxpool() {
        return databaseMaxpool;
    }

    public void setDatabaseMaxpool(Integer databaseMaxpool) {
        this.databaseMaxpool = databaseMaxpool;
    }

    public Integer getDatabaseMinpool() {
        return databaseMinpool;
    }

    public void setDatabaseMinpool(Integer databaseMinpool) {
        this.databaseMinpool = databaseMinpool;
    }

    public Integer getMetadataMaxpool() {
        return metadataMaxpool;
    }

    public void setMetadataMaxpool(Integer metadataMaxpool) {
        this.metadataMaxpool = metadataMaxpool;
    }

    public Integer getMetadataMinpool() {
        return metadataMinpool;
    }

    public void setMetadataMinpool(Integer metadataMinpool) {
        this.metadataMinpool = metadataMinpool;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Integer getDebugDataBase() {
        return debugDataBase;
    }

    public void setDebugDataBase(Integer debugDataBase) {
        this.debugDataBase = debugDataBase;
    }

    public String getLicenseServer() {
        return licenseServer;
    }

    public void setLicenseServer(String licenseServer) {
        this.licenseServer = licenseServer;
    }

    public String getExtraConfiguration() {
        return extraConfiguration;
    }

    public void setExtraConfiguration(String extraConfiguration) {
        this.extraConfiguration = extraConfiguration;
    }

    public String getDbExtra() {
        return dbExtra;
    }

    public void setDbExtra(String dbExtra) {
        this.dbExtra = dbExtra;
    }

    public String getTypeAuthentication() {
        return typeAuthentication;
    }

    public void setTypeAuthentication(String typeAuthentication) {
        this.typeAuthentication = typeAuthentication;
    }

    public String getXmlAuthentication() {
        return xmlAuthentication;
    }

    public void setXmlAuthentication(String xmlAuthentication) {
        this.xmlAuthentication = xmlAuthentication;
    }

    public String getMetadataFile() {
        return metadataFile;
    }

    public void setMetadataFile(String metadataFile) {
        this.metadataFile = metadataFile;
    }

    public String getLuthierProviderService() {
        return luthierProviderService;
    }

    public void setLuthierProviderService(String luthierProviderService) {
        this.luthierProviderService = luthierProviderService;
    }

    public Boolean getDisableLibs() {
        return disableLibs;
    }

    public void setDisableLibs(Boolean disableLibs) {
        this.disableLibs = disableLibs;
    }

    public List<PortalLuthierServiceProviderDTO> getServiceProvidersDataCollection() {
        return serviceProvidersDataCollection;
    }

    public void setServiceProvidersDataCollection(List<PortalLuthierServiceProviderDTO> serviceProvidersDataCollection) {
        this.serviceProvidersDataCollection = serviceProvidersDataCollection;
    }

    public String getTypeDataBase() {
        return typeDataBase;
    }

    public void setTypeDataBase(String typeDataBase) {
        this.typeDataBase = typeDataBase;
    }

    public String getDataBase() {
        return dataBase;
    }

    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUserDataBase() {
        return userDataBase;
    }

    public void setUserDataBase(String userDataBase) {
        this.userDataBase = userDataBase;
    }

    public String getPassDataBase() {
        return passDataBase;
    }

    public void setPassDataBase(String passDataBase) {
        this.passDataBase = passDataBase;
    }

    public Boolean getHasStorageProvider() {
        return hasStorageProvider;
    }

    public void setHasStorageProvider(Boolean hasStorageProvider) {
        this.hasStorageProvider = hasStorageProvider;
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

    public boolean isAbleToAudit() {
        return ableToAudit;
    }

    public void setAbleToAudit(boolean ableToAudit) {
        this.ableToAudit = ableToAudit;
    }

    public String getLuthierUser() {
        return luthierUser;
    }

    public void setLuthierUser(String luthierUser) {
        this.luthierUser = luthierUser;
    }

    public String getLuthierPassword() {
        return luthierPassword;
    }

    public void setLuthierPassword(String luthierPassword) {
        this.luthierPassword = luthierPassword;
    }

    @JsonIgnore
    public MetadadosBranchBean toBean() {
        MetadadosBranchBean bean = new MetadadosBranchBean();
        bean.setDbType(DatabaseTypeEnum.fromValue(this.typeDataBase));
        bean.setHost(this.server);
        bean.setDatabase(this.dataBase);
        bean.setDbUser(this.userDataBase);
        bean.setDbPassword(this.passDataBase);
        bean.setDebugId(this.debugDataBase);
        bean.setLicenseServer(this.licenseServer);
        bean.setLthUser(this.luthierUser);
        bean.setLthPassword(this.luthierPassword);
        return bean;
    }
}

