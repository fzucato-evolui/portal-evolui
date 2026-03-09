package br.com.evolui.portalevolui.web.rest.dto.ax;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtro para busca de projetos. Todos os campos são opcionais.")
public class ProjectFilterDTO {

    @Schema(description = "Identificador exato do projeto", example = "tributos")
    private String identifier;

    @Schema(description = "Repositório exato do projeto sem a organização", example = "tributos")
    private String repository;

    @Schema(description = "Descrição do projeto (busca parcial, ignorando maiúsculas/minúsculas e acentos)", example = "módulo")
    private String description;

    @Schema(description = "Título do projeto (busca parcial, ignorando maiúsculas/minúsculas e acentos)", example = "portal")
    private String title;

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
