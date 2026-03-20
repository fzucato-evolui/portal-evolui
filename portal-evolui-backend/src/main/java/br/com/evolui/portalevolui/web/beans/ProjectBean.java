package br.com.evolui.portalevolui.web.beans;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.dto.IconDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonView(JsonViewerPattern.Admin.class)
@Schema(description = "Projeto que agrupa módulos de compilação. Cada projeto está associado a um repositório GitHub "
        + "e pode conter múltiplos módulos organizados em uma estrutura hierárquica de vínculos (childBonds).")
public class ProjectBean {

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="project_sequence_gen")
    @SequenceGenerator(name="project_sequence_gen", sequenceName="project_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    @Schema(description = "ID do projeto", example = "65")
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    @Schema(description = "Identificador único textual do projeto", example = "plenariomonorepo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;

    @Column(name = "description", nullable = false, length = 500)
    @Schema(description = "Descrição do projeto", example = "Projeto Plenário", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Column(name = "title", nullable = false)
    @Schema(description = "Título de exibição do projeto", example = "Projeto Plenário", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Column(name = "icon")
    @Schema(description = "Ícone do projeto (fontSet + fontIcon)")
    private String icon;

    @Column(name = "framework", nullable = false)
    @Schema(description = "Indica se o projeto é um framework", example = "false")
    private boolean framework;

    @Column(name = "repository", nullable = false, unique = true)
    @Schema(description = "Nome do repositório de workflow GitHub associado ao projeto", example = "workflow-plenario-full", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repository;

    @Column(name = "luthier_project", nullable = false)
    @Schema(description = "Indica se o projeto é do tipo Luthier. Projetos Luthier exigem exatamente um módulo principal e um módulo de framework", example = "true")
    private boolean luthierProject;

    @OneToMany(mappedBy = "project",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    @Schema(description = "Lista flat de todos os módulos do projeto. Cada módulo pode possuir vínculos hierárquicos "
            + "com outros módulos através do campo 'bond' (pai) e 'childBonds' (filhos)")
    @OrderBy("id ASC")
    private List<ProjectModuleBean> modules;

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

    public List<ProjectModuleBean> getModules() {
        return modules;
    }

    public void setModules(List<ProjectModuleBean> modules) {
        if (modules != null) {
            for(ProjectModuleBean b : modules) {
                b.setProject(this);
            }
        }
        this.modules = modules;
    }

    public void addModule(ProjectModuleBean module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();
        }
        module.setProject(this);
        this.modules.add(module);
    }

    public boolean isFramework() {
        return framework;
    }

    public void setFramework(boolean framework) {
        this.framework = framework;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public boolean isLuthierProject() {
        return luthierProject;
    }

    public void setLuthierProject(boolean luthierProject) {
        this.luthierProject = luthierProject;
    }
}
