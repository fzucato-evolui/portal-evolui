package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteModuloConfigDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "environment_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_module_fk", "environment_fk"}, name = "ux_environment_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
public class AmbienteModuloBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "environment_module_sequence_gen")
    @SequenceGenerator(name = "environment_module_sequence_gen", sequenceName = "environment_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_module_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier", "title", "main", "framework"})
    private ProjectModuleBean projectModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private AmbienteBean ambiente;

    @JsonView(JsonViewerPattern.Super.class)
    @Column(name = "config", nullable = true, length = 5000)
    @Basic(fetch = FetchType.LAZY)
    protected String config;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectModuleBean getProjectModule() {
        return projectModule;
    }

    public void setProjectModule(ProjectModuleBean projectModule) {
        this.projectModule = projectModule;
    }

    public AmbienteBean getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteBean ambiente) {
        this.ambiente = ambiente;
    }

    public AmbienteModuloConfigDTO getConfig() {
        return AmbienteModuloConfigDTO.fromJson(this.config);
    }

    public void setConfig(AmbienteModuloConfigDTO config) {
        this.config = config.toJson();
    }
}
