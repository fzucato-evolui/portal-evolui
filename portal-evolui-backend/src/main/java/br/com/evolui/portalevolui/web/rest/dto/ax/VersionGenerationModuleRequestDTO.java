package br.com.evolui.portalevolui.web.rest.dto.ax;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Módulo incluído na geração de versão. Cada módulo é identificado pelo seu ID numérico (campo id do ProjectModuleBean). "
        + "Deve informar a tag e ao menos branch ou commit (um dos dois é obrigatório). "
        + "O repositório e o caminho relativo (relativePath) são obtidos automaticamente a partir do cadastro do módulo no projeto (ProjectModuleBean), "
        + "não sendo mais necessário informá-los na requisição. "
        + "Módulos vinculados (com bond) devem ter o mesmo repositório — esta validação é feita no salvamento do projeto. "
        + "Quando o módulo possuir vínculo de compilação (bond), o módulo pai (bond) também deve estar presente na lista de módulos da requisição, "
        + "e ambos devem, obrigatoriamente, ter os mesmos valores de branch e commit.")
public class VersionGenerationModuleRequestDTO {

    @Schema(description = "ID numérico do módulo do projeto",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Branch do repositório Git. Obrigatório se commit não for informado", example = "main")
    private String branch;

    @Schema(description = "Hash do commit Git. Obrigatório se branch não for informada", example = "a1b2c3d4")
    private String commit;

    @JsonIgnore
    private String tag;

    private boolean enabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
