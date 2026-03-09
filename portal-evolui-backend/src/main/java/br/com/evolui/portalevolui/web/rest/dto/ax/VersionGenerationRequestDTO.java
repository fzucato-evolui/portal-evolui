package br.com.evolui.portalevolui.web.rest.dto.ax;

import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Requisição para geração de versão")
public class VersionGenerationRequestDTO {

    @Schema(description = "ID numérico do projeto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Tipo de compilação", example = "patch")
    private CompileTypeEnum compileType;

    @Schema(description = "Tag da versão referente ao módulo principal. As 2 primeiras posições devem ser numéricas e a terceira numérica + qualifiter opcional. ", example = "10.0.1-rc3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tag;

    @Schema(description = "Lista de módulos que compõem a geração de versão")
    private List<VersionGenerationModuleRequestDTO> modules;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompileTypeEnum getCompileType() {
        return compileType;
    }

    public void setCompileType(CompileTypeEnum compileType) {
        this.compileType = compileType;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<VersionGenerationModuleRequestDTO> getModules() {
        return modules;
    }

    public void setModules(List<VersionGenerationModuleRequestDTO> modules) {
        this.modules = modules;
    }
}
