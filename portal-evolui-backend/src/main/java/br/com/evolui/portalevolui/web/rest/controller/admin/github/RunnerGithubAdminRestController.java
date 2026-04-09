package br.com.evolui.portalevolui.web.rest.controller.admin.github;

import br.com.evolui.portalevolui.shared.dto.RunnerInstallerConnectionDTO;
import br.com.evolui.portalevolui.shared.util.GeradorTokenPortalEvolui;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerRegistrationTokenDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.runner.ActionsRunnerLatestDownloadDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/github/runner")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class RunnerGithubAdminRestController {

    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;

    @Autowired
    private GithubVersionService service;

    @GetMapping()
    public ResponseEntity<List<GithubRunnerDTO>> get() throws Exception {
        try {
            List<GithubRunnerDTO> resp = new ArrayList<>();
            if (this.service.initialize()) {
                resp = this.service.getRunners().getRunners();
            }
            return ResponseEntity.ok(resp);
        } finally {
            this.service.dispose();
        }
    }

    /**
     * Token criptografado para o client Go: uuid, host, destination (JWT do usuário — mesmo sufixo de fila STOMP do browser).
     */
    //@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @PostMapping("/generate-token/{uuid}")
    public ResponseEntity<LinkedHashMap<String, String>> generateToken(@PathVariable String uuid, HttpServletRequest request) throws Exception {
        String jwt = resolveBearerToken(request);
        if (!StringUtils.hasText(jwt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization Bearer ausente");
        }
        ObjectMapper mapper = new ObjectMapper();
        RunnerInstallerConnectionDTO conn = new RunnerInstallerConnectionDTO();
        conn.setUuid(uuid);
        conn.setHost(this.baseUrl + ":" + this.port);
        conn.setDestination(jwt);

        LinkedHashMap<String, String> resp = new LinkedHashMap<>();
        resp.put("token", GeradorTokenPortalEvolui.encrypt(mapper.writeValueAsString(conn)));
        resp.put("endpoint", conn.getHost());
        return ResponseEntity.ok(resp);
    }

    /**
     * Chamado no passo “Instalar agora”; token de curta duração da API GitHub.
     */
    @PostMapping("/registration-token")
    public ResponseEntity<GithubRunnerRegistrationTokenDTO> createRegistrationToken() throws Exception {
        try {
            if (!service.initialize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub não configurado");
            }
            return ResponseEntity.ok(service.createOrganizationRunnerRegistrationToken());
        } finally {
            service.dispose();
        }
    }

    @GetMapping("/actions-runner-latest")
    public ResponseEntity<ActionsRunnerLatestDownloadDTO> actionsRunnerLatest(@RequestParam("os") String os) throws Exception {
        try {
            if (!service.initialize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub não configurado");
            }
            return ResponseEntity.ok(service.resolveLatestActionsRunnerDownload(os));
        } finally {
            service.dispose();
        }
    }

    @GetMapping("/runner-name-available")
    public ResponseEntity<Map<String, Boolean>> runnerNameAvailable(@RequestParam("name") String name) throws Exception {
        try {
            if (!service.initialize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub não configurado");
            }
            boolean available = service.isRunnerNameAvailable(name);
            return ResponseEntity.ok(Map.of("available", available));
        } finally {
            service.dispose();
        }
    }

    /**
     * Remove o runner apenas da organização na API GitHub (não desinstala o software na máquina).
     */
    //@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{runnerId}")
    public ResponseEntity<Void> deleteRunner(@PathVariable long runnerId) throws Exception {
        try {
            if (!service.initialize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub não configurado");
            }
            service.deleteOrganizationRunner(runnerId);
            return ResponseEntity.noContent().build();
        } finally {
            service.dispose();
        }
    }

    /**
     * Token para executar {@code config.sh remove} / {@code config.cmd remove} na pasta de instalação do runner.
     */
    //@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @PostMapping("/remove-token")
    public ResponseEntity<GithubRunnerRegistrationTokenDTO> createRemoveToken() throws Exception {
        try {
            if (!service.initialize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub não configurado");
            }
            return ResponseEntity.ok(service.createOrganizationRunnerRemoveToken());
        } finally {
            service.dispose();
        }
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (StringUtils.hasText(h) && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }
}
