package br.com.evolui.portalevolui.web.rest.controller.admin.github;

import br.com.evolui.portalevolui.web.rest.dto.github.GithubContentDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubDetailedRepositoryDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/github/repository")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class RepositoryGithubAdminRestController {

    @Autowired
    private GithubVersionService service;

    @GetMapping()
    public ResponseEntity<List<GithubDetailedRepositoryDTO>> get() throws Exception {
        try {
            List<GithubDetailedRepositoryDTO> resp = new ArrayList<>();
            if (this.service.initialize()) {
                resp = this.service.getRepositories();
            }
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }
    @GetMapping("/workflow")
    public ResponseEntity<List<GithubDetailedRepositoryDTO>> getWorkflowRepositories() throws Exception {
        try {
            List<GithubDetailedRepositoryDTO> resp = new ArrayList<>();
            if (this.service.initialize()) {
                resp = this.service.getWorflowRepositories();
            }
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/{repository}/contents")
    public ResponseEntity<List<GithubContentDTO>> getContents(
            @PathVariable("repository") String repository,
            @RequestParam(value = "path", defaultValue = "") String path) throws Exception {
        try {
            List<GithubContentDTO> resp = new ArrayList<>();
            if (this.service.initialize()) {
                resp = this.service.listContentsAtPath(repository, path, GithubVersionService.ContentType.ALL);
            }
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }
}
