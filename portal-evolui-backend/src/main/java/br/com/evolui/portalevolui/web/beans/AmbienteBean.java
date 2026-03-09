package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "environment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
public class AmbienteBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "environment_sequence_gen")
    @SequenceGenerator(name = "environment_sequence_gen", sequenceName = "environment_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier", "title", "main", "framework"})
    private ProjectBean project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ClienteBean client;

    @Formula("(SELECT COUNT(u.id) FROM version_update u WHERE u.environment_fk = id AND u.status <> 2)")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long busy;

    @OneToMany(mappedBy = "ambiente",
            cascade = CascadeType.ALL
    )
    private List<AmbienteModuloBean> modules = new ArrayList<>();

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

    public ClienteBean getClient() {
        return this.client;
    }

    public void setClient(ClienteBean client) {
        this.client = client;
    }

    public boolean getBusy() {
        return busy != null && busy > 0L;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean produto) {
        this.project = produto;
    }

    public List<AmbienteModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<AmbienteModuloBean> modules) {
        if (modules != null && !modules.isEmpty()) {
            for(AmbienteModuloBean b : modules) {
                b.setAmbiente(this);
            }
        }
        this.modules = modules;
    }

    public void addModule(AmbienteModuloBean module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();

        }
        module.setAmbiente(this);
        this.modules.add(module);
    }

}
