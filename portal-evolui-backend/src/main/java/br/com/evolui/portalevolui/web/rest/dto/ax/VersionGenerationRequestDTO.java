package br.com.evolui.portalevolui.web.rest.dto.ax;

import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Schema(description = "Email do usuário logado. Se não informado ou se o usuário não existir na base do IDP, será usado o usuário da autenticação", example = "ze@evoluitecnologia.com.br")
    private String user;

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

    /**
     * Tag normalizado para o formato do IDP (major.minor.patch.build).
     * O AX versiona com 3 posições e, para patch, incrementa a 3ª posição (ex.: 26.3.1). No IDP a 3ª
     * posição é parte da branch e a build é a 4ª; então, para compileType=patch com tag de 3 posições
     * numéricas, zera-se a 3ª posição (26.3.1 -> 26.3.0) para casar com a branch existente, deixando o
     * IDP incrementar a build.
     */
    @JsonIgnore
    public String getNormalizedTag() {
        if (this.compileType == CompileTypeEnum.patch
                && this.tag != null
                && this.tag.matches("^\\d+\\.\\d+\\.\\d+$")) {
            String[] parts = this.tag.split("\\.");
            return parts[0] + "." + parts[1] + ".0";
        }
        return this.tag;
    }

    public List<VersionGenerationModuleRequestDTO> getModules() {
        return modules;
    }

    public void setModules(List<VersionGenerationModuleRequestDTO> modules) {
        this.modules = modules;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
