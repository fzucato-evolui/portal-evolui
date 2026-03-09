package br.com.evolui.portalevolui.web.rest.controller.admin.github;

import br.com.evolui.portalevolui.web.rest.dto.github.GithubMemberDTO;
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
@RequestMapping("/api/admin/github/member")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class MemberGithubAdminRestController {

    @Autowired
    private GithubVersionService service;

    @GetMapping()
    public ResponseEntity<List<GithubMemberDTO>> get() throws Exception {
        try {
            List<GithubMemberDTO> resp = new ArrayList<>();
            if (this.service.initialize()) {
                resp = this.service.getMembers();
            }
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }
}
