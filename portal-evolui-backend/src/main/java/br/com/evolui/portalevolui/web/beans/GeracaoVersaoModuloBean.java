package br.com.evolui.portalevolui.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "version_generation_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_module_fk", "version_generation_fk"}, name = "ux_ver_gen_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GeracaoVersaoModuloBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "version_generation_module_module_sequence_gen")
    @SequenceGenerator(name = "version_generation_module_sequence_gen", sequenceName = "version_generation_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_module_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier"})
    private ProjectModuleBean projectModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_generation_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private GeracaoVersaoBean geracaoVersao;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Transient
    private String repositoryBranch;

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

    public GeracaoVersaoBean getGeracaoVersao() {
        return geracaoVersao;
    }

    public void setGeracaoVersao(GeracaoVersaoBean geracaoVersao) {
        this.geracaoVersao = geracaoVersao;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRepositoryBranch() {
        return repositoryBranch;
    }

    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
    }
}
