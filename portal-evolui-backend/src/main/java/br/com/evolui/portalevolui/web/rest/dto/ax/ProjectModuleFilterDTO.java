package br.com.evolui.portalevolui.web.rest.dto.ax;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtro para busca de módulos de projetos. Todos os campos são opcionais.")
public class ProjectModuleFilterDTO {

    @Schema(description = "Identificador exato do módulo", example = "tributos")
    private String identifier;

    @Schema(description = "Descrição do módulo (busca parcial, ignorando maiúsculas/minúsculas e acentos)", example = "módulo")
    private String description;

    @Schema(description = "Título do módulo (busca parcial, ignorando maiúsculas/minúsculas e acentos)", example = "portal")
    private String title;

    @Schema(description = "Se o módulo é principal (true) ou não (false)")
    private Boolean main;

    @Schema(description = "Repositório Git do módulo (busca exata)", example = "plenario-full")
    private String repository;

    @Schema(description = "Caminho relativo de compilação (busca exata)", example = "plenario/nube")
    private String relativePath;

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

    public Boolean getMain() {
        return main;
    }

    public void setMain(Boolean main) {
        this.main = main;
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
