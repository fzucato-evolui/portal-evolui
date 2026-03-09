package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.ClienteBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchClienteBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.repository.client.ClienteRepository;
import br.com.evolui.portalevolui.web.repository.metadados.MetadadosBranchRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.rest.dto.version.AvailableVersionDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/metadados")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class MetadadosAdminRestController {

    private String target;
    @Autowired
    protected GithubVersionService service;
    @Autowired
    ClienteRepository clientRepository;

    @Autowired
    MetadadosBranchRepository repository;

    @Autowired
    ProjectRepository projectRepository;
    

    @GetMapping("/{project}/all")
    public ResponseEntity<List<MetadadosBranchBean>> getAll(@PathVariable("project") String project) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findAllByProjectIdentifier(project));
    }

    @GetMapping("/{project}/{id}")
    public ResponseEntity<MetadadosBranchBean> get(@PathVariable("project") String project, @PathVariable("id") Long id) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{project}")
    public ResponseEntity<MetadadosBranchBean> save(@PathVariable("project") String project, @RequestBody MetadadosBranchBean body) throws Exception{
        this.target = project;
        ProjectBean projectBean = projectRepository.findByIdentifier(this.target).orElse(null);
        if (projectBean == null) {
            throw new Exception("Projeto não encontrado");
        }
        body.setProject(projectBean);
        if (!projectBean.isLuthierProject()) {
            throw new Exception("Apenas projetos do tipo Luthier podem ter Metadados de Versão");
        }
        if (body.getId() != null && body.getId() > 0) {
            MetadadosBranchBean bean = this.repository.findById(body.getId()).orElse(null);
            if (bean.getClients() != null && !bean.getClients().isEmpty()) {
                for (MetadadosBranchClienteBean metaClient : bean.getClients()) {
                    Long id = metaClient.getId();
                    if (body.getClients() == null || body.getClients().isEmpty() ||
                            !body.getClients().stream().anyMatch(x -> x.getId() != null && x.getId().equals(id))) {
                        this.repository.deleteMetaBranchClientBeanById(id);
                    }
                }
            }
        }

        return ResponseEntity.ok(this.repository.save(body));
    }

    @DeleteMapping("/{project}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }


    @GetMapping("/{project}/initial-data")
    public ResponseEntity<LinkedHashMap<String, Object>> getBranches(@PathVariable("project") String project) throws Exception {
        try {
            this.target = project;
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
            resp.put("branches", AvailableVersionDTO.parseFromGithubBranches(this.service.getAllBranches(repository)));
            resp.put("clients", this.getClients());
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }

    private List<ClienteBean> getClients() {
        return this.clientRepository.findAllByProjectIdentifier(this.target);
    }
}
