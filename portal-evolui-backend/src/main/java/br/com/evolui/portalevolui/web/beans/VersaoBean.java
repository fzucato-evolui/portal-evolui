package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "available_version", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_fk", "major", "minor", "patch", "build"}, name = "ux_available_version")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VersaoBean extends VersaoBuildBaseBean implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="available_version_sequence_gen")
    @SequenceGenerator(name="available_version_sequence_gen", sequenceName="available_version_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIncludeProperties(value = {"id"})
    private ProjectBean project;

    @OneToMany(mappedBy = "versao",
            cascade = CascadeType.ALL
    )
    private List<VersaoModuloBean> modules = new ArrayList<>();

    @Column(name = "version_type")
    private CompileTypeEnum versionType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean produto) {
        this.project = produto;
    }

    public List<VersaoModuloBean> getModules() {
        return modules;
    }

    public void setModules(List<VersaoModuloBean> modules) {
        if (modules != null && !modules.isEmpty()) {
            for(VersaoModuloBean b : modules) {
                b.setVersao(this);
            }
        }
        this.modules = modules;
    }

    public void addModule(VersaoModuloBean module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();

        }
        module.setVersao(this);
        this.modules.add(module);
    }

    public CompileTypeEnum getVersionType() {
        if (versionType == null) {
            if (StringUtils.hasText(this.getQualifier())) {
                if (this.getQualifier().toLowerCase().contains("beta")) {
                    return CompileTypeEnum.beta;
                }
            }
            else if (this.getBuild().equals("0")) {
                return CompileTypeEnum.stable;
            }
            return CompileTypeEnum.patch;
        }
        return versionType;
    }

    public void setVersionType(CompileTypeEnum versionType) {
        this.versionType = versionType;
    }
}
