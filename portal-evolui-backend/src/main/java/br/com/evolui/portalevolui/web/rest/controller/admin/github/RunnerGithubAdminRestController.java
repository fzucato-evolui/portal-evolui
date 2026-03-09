package br.com.evolui.portalevolui.web.rest.controller.admin.github;

import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/github/runner")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class RunnerGithubAdminRestController {

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
        }
        finally {
            this.service.dispose();
        }
    }
}
