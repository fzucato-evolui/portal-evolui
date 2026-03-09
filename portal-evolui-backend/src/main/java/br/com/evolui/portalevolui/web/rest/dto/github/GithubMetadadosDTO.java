package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchClienteBean;
import br.com.evolui.portalevolui.web.beans.enums.DatabaseTypeEnum;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class GithubMetadadosDTO {
    private String host;
    private Integer port;
    private Integer debugId;
    private String database;
    private String dbUser;
    private String dbPassword;
    private String lthUser;
    private String lthPassword;
    private String licenseServer;
    private String jvmOptions;
    private DatabaseTypeEnum dbType;
    private List<GithubClienteDTO> clients;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getDebugId() {
        return debugId;
    }

    public void setDebugId(Integer debugId) {
        this.debugId = debugId;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getLthUser() {
        return lthUser;
    }

    public void setLthUser(String lthUser) {
        this.lthUser = lthUser;
    }

    public String getLthPassword() {
        return lthPassword;
    }

    public void setLthPassword(String lthPassword) {
        this.lthPassword = lthPassword;
    }

    public String getLicenseServer() {
        return licenseServer;
    }

    public void setLicenseServer(String licenseServer) {
        this.licenseServer = licenseServer;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public DatabaseTypeEnum getDbType() {
        return dbType;
    }

    public void setDbType(DatabaseTypeEnum dbType) {
        this.dbType = dbType;
    }

    public List<GithubClienteDTO> getClients() {
        return clients;
    }

    public void setClients(List<GithubClienteDTO> clients) {
        this.clients = clients;
    }

    public void addClient(GithubClienteDTO client) {
        if(this.clients == null) {
            this.clients = new ArrayList<>();
        }
        this.clients.add(client);
    }

    public static GithubMetadadosDTO fromBean(MetadadosBranchBean bean) {
        if (bean == null) {
            return null;
        }
        GithubMetadadosDTO dto = new GithubMetadadosDTO();
        BeanUtils.copyProperties(bean, dto);
        if (bean.getClients() != null) {
            for(MetadadosBranchClienteBean c : bean.getClients()) {
                GithubClienteDTO client = GithubClienteDTO.fromBean(c.getClient());
                if (client != null) {
                    dto.addClient(GithubClienteDTO.fromBean(c.getClient()));
                }
            }
        }
        return dto;
    }
}
