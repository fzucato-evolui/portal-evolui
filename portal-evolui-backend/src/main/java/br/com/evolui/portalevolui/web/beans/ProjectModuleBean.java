package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.dto.IconDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_fk", "identifier"}, name = "ux_project_module")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
@Schema(description = "Módulo pertencente a um projeto. Módulos podem ter vínculos hierárquicos (N níveis de profundidade) "
        + "através de uma auto-referência: 'bond' aponta para o módulo pai e 'childBonds' lista os módulos filhos. "
        + "Módulos vinculados compartilham o mesmo projeto e, na geração de versão, devem obrigatoriamente "
        + "ser compilados dentro do mesmo repositório.")
public class ProjectModuleBean {

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="project_module_sequence_gen")
    @SequenceGenerator(name="project_module_sequence_gen", sequenceName="project_module_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    @Schema(description = "ID do módulo", example = "95")
    private Long id;

    @Column(name = "identifier", nullable = false)
    @Schema(description = "Identificador único do módulo dentro do projeto. Derivado da estrutura de workflows do repositório GitHub",
            example = "plenarioMobile", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;

    @Column(name = "description", nullable = false, length = 500)
    @Schema(description = "Descrição do módulo", example = "Mobile", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Column(name = "title", nullable = false)
    @Schema(description = "Título de exibição do módulo", example = "Mobile", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Column(name = "icon")
    @Schema(description = "Ícone do módulo (fontSet + fontIcon)")
    private String icon;

    @Column(name = "main", nullable = false)
    @Schema(description = "Indica se é o módulo principal do projeto. Apenas um módulo principal é permitido por projeto", example = "false")
    private boolean main;

    @Column(name = "framework", nullable = false)
    @Schema(description = "Indica se é o módulo de framework do projeto. Apenas um módulo framework é permitido por projeto", example = "false")
    private boolean framework;

    @JoinColumn(name = "project_fk", referencedColumnName = "id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIncludeProperties({"id", "identifier", "repository", "title"})
    @Schema(description = "Projeto ao qual este módulo pertence. Todos os módulos de uma hierarquia de childBonds compartilham o mesmo projeto")
    private ProjectBean project;

    @OneToMany(mappedBy = "bond", fetch = FetchType.LAZY)
    @Schema(description = "Lista de módulos filhos vinculados a este módulo. Relacionamento navegacional (sem cascade). "
            + "A remoção de um módulo pai implica na remoção de todos os seus descendentes")
    private List<ProjectModuleBean> childBonds;

    @JoinColumn(name = "bond", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIncludeProperties({"id", "identifier", "repository"})
    @Schema(description = "Módulo pai na hierarquia de vínculos. Nulo para módulos raiz (sem vínculo com outro módulo)")
    private ProjectModuleBean bond;

    @Column(name = "repository", nullable = false)
    @Schema(description = "Repositório Git do módulo. Módulos vinculados (com bond) devem obrigatoriamente ter o mesmo repositório "
            + "que o módulo pai do vínculo. Esta validação é aplicada no salvamento do projeto e também bloqueia a geração de versão",
            example = "plenario-full", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repository;

    @Column(name = "relative_path", length = 2000)
    @Schema(description = "Caminho relativo dentro do repositório para a compilação. Se não informado, será considerada a raiz do repositório",
            example = "plenario/nube")
    private String relativePath;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IconDTO getIcon() {
        return IconDTO.fromJson(this.icon);
    }

    public void setIcon(IconDTO icon) {
        this.icon = icon.toJson();
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    public ProjectBean getProject() {
        return project;
    }

    public void setProject(ProjectBean parent) {
        this.project = parent;
    }

    public boolean isFramework() {
        return framework;
    }

    public void setFramework(boolean framework) {
        this.framework = framework;
    }

    public List<ProjectModuleBean> getChildBonds() {
        return childBonds;
    }

    public void addBond(ProjectModuleBean bond) {
        if (this.childBonds == null) {
            this.childBonds = new ArrayList<>();
        }
        bond.setBond(this);
        this.childBonds.add(bond);
    }

    public void setChildBonds(List<ProjectModuleBean> childBonds) {
        if (childBonds != null) {
            for(ProjectModuleBean b : childBonds) {
                b.setBond(this);
            }
        }
        this.childBonds = childBonds;
    }

    public ProjectModuleBean getBond() {
        return bond;
    }

    public void setBond(ProjectModuleBean bond) {
        this.bond = bond;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}
