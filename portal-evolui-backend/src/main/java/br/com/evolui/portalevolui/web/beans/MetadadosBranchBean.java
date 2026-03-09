package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.DatabaseTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_project", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_fk", "major", "minor", "patch"}, name = "ux_available_version")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MetadadosBranchBean extends VersaoBranchBaseBean {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="meta_project_sequence_gen")
    @SequenceGenerator(name="meta_project_sequence_gen", sequenceName="meta_project_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private ProjectBean project;

    @Column(name = "host", nullable = false, length = 500)
    protected String host;

    @Column(name = "port", nullable = true)
    protected Integer port;

    @Column(name = "debug_id", nullable = false)
    protected Integer debugId;

    @Column(name = "database", nullable = true, length = 255)
    protected String database;

    @Column(name = "db_user", nullable = false, length = 255)
    protected String dbUser;

    @Column(name = "db_password", nullable = true, length = 255)
    protected String dbPassword;

    @Column(name = "lth_user", nullable = false, length = 255)
    protected String lthUser;

    @Column(name = "lth_password", nullable = false, length = 255)
    protected String lthPassword;

    @Column(name = "license_server", nullable = false, length = 255)
    private String licenseServer;

    @Column(name = "jvm_options", nullable = true, length = 255)
    private String jvmOptions;

    @Column(name = "db_type", nullable = false)
    private DatabaseTypeEnum dbType;
    
    @OneToMany(mappedBy = "metadados",
            cascade = CascadeType.ALL
    )
    private List<MetadadosBranchClienteBean> clients = new ArrayList<>();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MetadadosBranchClienteBean> getClients() {

        return clients;
    }

    public void setClients(List<MetadadosBranchClienteBean> clients) {
        if (clients != null) {
            for(MetadadosBranchClienteBean b : clients) {
                b.setMetadados(this);
            }
        }
        this.clients = clients;
    }

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

    public void setDbUser(String user) {
        this.dbUser = user;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String password) {
        this.dbPassword = password;
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

    public void setLthPassword(String lth_password) {
        this.lthPassword = lth_password;
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

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean project) {
        this.project = project;
    }
}
