package br.com.evolui.portalevolui.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "available_version_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_module_fk", "available_version_fk"}, name = "ux_available_version_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VersaoModuloBean extends VersaoBuildBaseBean {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "available_version_module_sequence_gen")
    @SequenceGenerator(name = "available_version_module_sequence_gen", sequenceName = "available_version_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_module_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id", "identifier", "title"})
    private ProjectModuleBean projectModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "available_version_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private VersaoBean versao;

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

    public VersaoBean getVersao() {
        return versao;
    }

    public void setVersao(VersaoBean versao) {
        this.versao = versao;
    }
}
