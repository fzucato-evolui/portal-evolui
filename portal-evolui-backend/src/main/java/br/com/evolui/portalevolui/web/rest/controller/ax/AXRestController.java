package br.com.evolui.portalevolui.web.rest.controller.ax;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.beans.enums.UserTypeEnum;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.rest.controller.admin.GeracaoVersaoAdminRestController;
import br.com.evolui.portalevolui.web.rest.controller.admin.VersaoAdminRestController;
import br.com.evolui.portalevolui.web.rest.controller.publico.AuthPublicRestController;
import br.com.evolui.portalevolui.web.rest.dto.ax.LoginDTO;
import br.com.evolui.portalevolui.web.rest.dto.ax.ProjectFilterDTO;
import br.com.evolui.portalevolui.web.rest.dto.ax.ProjectModuleFilterDTO;
import br.com.evolui.portalevolui.web.rest.dto.ax.VersionGenerationRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "AX", description = "API de integração AX. Possui um endpoint público de login e endpoints autenticados para geração de versão e consulta de projetos. "
        + "Os endpoints autenticados requerem role SUPER, HYPER ou ADMIN e autenticação via Bearer Token no header Authorization. "
        + "O token é obtido através do endpoint de login (POST /api/public/ax/login).")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido no endpoint de login (POST /api/public/ax/login). "
                + "Informe o valor do campo 'accessToken' retornado no login."
)
public class AXRestController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GeracaoVersaoAdminRestController geracaoVersaoController;

    @Autowired
    private AuthPublicRestController authController;

    @Autowired
    private VersaoAdminRestController versaoController;

    @Operation(
            summary = "Login (público)",
            description = "Autentica o usuário e retorna um token JWT (Bearer Token). "
                    + "O token retornado no campo 'accessToken' deve ser utilizado no header 'Authorization: Bearer {token}' "
                    + "para acessar os endpoints autenticados desta API."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso. Retorna um objeto contendo "
                    + "'accessToken' (token JWT para autenticação) e 'user' (dados do usuário autenticado)"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/api/public/ax/login")
    public ResponseEntity<LinkedHashMap> authenticateUser(@RequestBody LoginDTO body) throws Exception {
        UserBean user = new UserBean();
        user.setLogin(body.getLogin());
        user.setPassword(body.getPassword());
        user.setUserType(UserTypeEnum.CUSTOM);
        return this.authController.authenticateUser(user);
    }

    @Operation(
            summary = "Gerar versão",
            description = "Dispara o processo de geração de versão para o projeto e seus módulos. "
                    + "O projeto é identificado pelo seu ID numérico (campo id do ProjectBean). "
                    + "Cada módulo na lista é identificado pelo ID numérico do ProjectModuleBean. "
                    + "O repositório e o caminho relativo (relativePath) de cada módulo são obtidos automaticamente "
                    + "a partir do cadastro do módulo no projeto, não sendo passados na requisição.\n\n"
                    + "Hierarquia de tipos de compilação (do mais estável ao menos estável): "
                    + "patch > stable > rc > beta > alpha.\n\n"
                    + "O campo compileType é opcional. Quando não informado (null), é auto-detectado:\n"
                    + "- Se não existem builds para a branch informada → stable. "
                    + "Neste caso, a branch da tag deve ser obrigatoriamente maior que a última versão estável do projeto.\n"
                    + "- Se já existem builds para a branch → patch (incrementa o build number).\n\n"
                    + "Regras por tipo:\n"
                    + "- patch: Requer build existente (não-transitória). Incrementa build number se módulo principal incluído.\n"
                    + "- stable: Bloqueia se patch/stable já existe. Módulo principal obrigatório. Build='0'.\n"
                    + "- rc/beta/alpha (transitórios): Bloqueiam tipos superiores na hierarquia. "
                    + "Se módulo principal não incluído, reutiliza tag do último build do mesmo tipo. "
                    + "Se incluído, cria nova versão com qualificador (RC/BETA/ALPHA).\n\n"
                    + "Módulos faltantes (não-framework não incluídos na requisição) são preenchidos automaticamente "
                    + "a partir da última versão disponível, com enabled=false.\n\n"
                    + "A tag do corpo da requisição refere-se sempre ao módulo principal. "
                    + "Módulos vinculados (com bond) devem ter o mesmo repositório que o módulo pai — "
                    + "esta validação é aplicada na gravação do projeto e re-validada na geração de versão. "
                    + "Quando um módulo possui vínculo de compilação (bond), seu módulo 'pai' também deve ser incluído na lista de módulos da requisição. "
                    + "Além disso, módulos vinculados devem obrigatoriamente ter os mesmos valores de branch e commit que seu módulo 'pai'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geração de versão criada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro de validação ou módulo não encontrado",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/api/admin/ax/version-generation")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GeracaoVersaoBean> generate(@RequestBody VersionGenerationRequestDTO body) throws Exception {
        return this.geracaoVersaoController.generateFromRequest(body);
    }

    // ========== Outros endpoints ==========

    @Operation(
            summary = "Listar projetos",
            description = "Retorna todos os projetos não-framework, com seus módulos não-framework. "
                    + "Cada módulo inclui os campos 'repository' e 'relativePath' pré-configurados."
    )
    @ApiResponse(responseCode = "200", description = "Lista de projetos")
    @GetMapping("/api/admin/ax/projects")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProjectBean>> getAll() {
        List<ProjectBean> all = this.projectRepository.findAll();
        all = all.stream().filter(p -> !p.isFramework()).collect(Collectors.toList());
        all.forEach(p -> {
            if (p.getModules() != null) {
                p.setModules(p.getModules().stream().filter(m -> !m.isFramework()).collect(Collectors.toList()));
            }
        });
        return ResponseEntity.ok(all);
    }

    @Operation(
            summary = "Buscar projeto por ID",
            description = "Retorna os detalhes de um projeto específico, incluindo seus módulos filhos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
            @ApiResponse(responseCode = "200", description = "Projeto não encontrado (retorna null no body)")
    })
    @GetMapping("/api/admin/ax/project/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectBean> get(
            @Parameter(description = "ID do projeto", example = "1", required = true)
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(this.projectRepository.findById(id).orElse(null));
    }

    @GetMapping("/api/admin/ax/project-module/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectModuleBean> getProjectModule(
            @Parameter(description = "ID do módulo projeto", example = "1", required = true)
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(this.projectRepository.findModuleById(id).orElse(null));
    }

    @Operation(
            summary = "Buscar projetos com filtro",
            description = "Retorna uma lista de projetos de acordo com os filtros informados no body. "
                    + "Todos os campos do filtro são opcionais. "
                    + "O campo 'identifier' realiza busca exata. "
                    + "O campo 'repository' realiza busca exata. "
                    + "Os campos 'description' e 'title' realizam busca parcial (LIKE), ignorando maiúsculas/minúsculas e acentos. "
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos encontrados de acordo com o filtro")
    })
    @PostMapping("/api/admin/ax/project/filter")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProjectBean>> filter(@RequestBody ProjectFilterDTO body) {
        String description = body.getDescription() != null ? "%" + stripAccents(body.getDescription()).toLowerCase() + "%" : null;
        String title = body.getTitle() != null ? "%" + stripAccents(body.getTitle()).toLowerCase() + "%" : null;
        return ResponseEntity.ok(this.projectRepository.findByFilter(
                body.getIdentifier(),
                body.getRepository(),
                description,
                title
        ));
    }

    @Operation(
            summary = "Buscar módulos de projeto com filtro",
            description = "Retorna uma lista de módulos dos projetos de acordo com os filtros informados no body. "
                    + "Todos os campos do filtro são opcionais. "
                    + "O campo 'identifier' realiza busca exata. "
                    + "Os campos 'description' e 'title' realizam busca parcial (LIKE), ignorando maiúsculas/minúsculas e acentos. "
                    + "O campo 'main' filtra por módulo principal (true) ou módulo (false). "
                    + "O campo 'repository' realiza busca exata pelo repositório Git do módulo. "
                    + "O campo 'relativePath' realiza busca exata pelo caminho relativo de compilação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de módulos dos projetos encontrados de acordo com o filtro")
    })
    @PostMapping("/api/admin/ax/project-module/filter")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProjectModuleBean>> filterProjectModules(@RequestBody ProjectModuleFilterDTO body) {
        String description = body.getDescription() != null ? "%" + stripAccents(body.getDescription()).toLowerCase() + "%" : null;
        String title = body.getTitle() != null ? "%" + stripAccents(body.getTitle()).toLowerCase() + "%" : null;
        return ResponseEntity.ok(this.projectRepository.findModulesByFilter(
                body.getIdentifier(),
                description,
                title,
                body.getMain(),
                body.getRepository(),
                body.getRelativePath()
        ));
    }

    @Operation(
            summary = "Listar versões disponíveis de um projeto",
            description = "Retorna as versões disponíveis de um projeto com suporte a paginação, limite e ordenação. "
                    + "A ordenação é por número de versão (major, minor, patch, tipo, build)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de versões disponíveis do projeto"),
            @ApiResponse(responseCode = "500", description = "Projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/api/admin/ax/available-versions/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<VersaoBean>> getAvailableProjectVersions(
            @Parameter(description = "ID do projeto", example = "1", required = true)
            @PathVariable("id") Long id,
            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @Parameter(description = "Quantidade de itens por página", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "Direção da ordenação por versão: ASC ou DESC", example = "DESC")
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection) throws Exception {

        ProjectBean project = this.projectRepository.findById(id).orElse(null);
        if (project == null) {
            throw new Exception(String.format("Projeto com o ID %s não foi encontrado", id));
        }

        List<VersaoBean> versoes = new ArrayList<>(this.versaoController.getAll(project.getIdentifier()).getBody());
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            Collections.reverse(versoes);
        }
        int fromIndex = page * size;
        if (fromIndex >= versoes.size()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        int toIndex = Math.min(fromIndex + size, versoes.size());
        return ResponseEntity.ok(versoes.subList(fromIndex, toIndex));
    }

    private String stripAccents(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

}
