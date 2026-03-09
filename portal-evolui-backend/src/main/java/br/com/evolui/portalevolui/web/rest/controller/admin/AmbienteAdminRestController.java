package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.AmbienteBean;
import br.com.evolui.portalevolui.web.beans.ClienteBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.repository.ambiente.AmbienteRepository;
import br.com.evolui.portalevolui.web.repository.atualizacao_versao.AtualizacaoVersaoRepository;
import br.com.evolui.portalevolui.web.repository.client.ClienteRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerListDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ambiente")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class AmbienteAdminRestController {

    private String target;
    @Autowired
    protected GithubVersionService service;

    @Autowired
    ClienteRepository clientRepository;

    @Autowired
    AmbienteRepository repository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    VersaoRepository versaoRepository;

    @Autowired
    AtualizacaoVersaoRepository atualizacaoVersaoRepository;

    @JsonView(JsonViewerPattern.Admin.class)
    @GetMapping("/{project}/all")
    public ResponseEntity<List<AmbienteBean>> getAll(@PathVariable("project") String project) {
        this.target = project;
       return ResponseEntity.ok(this.repository.findAllByProjectIdentifier(project));
    }

    @GetMapping("/{project}/{id}")
    public ResponseEntity<AmbienteBean> get(@PathVariable("project") String project, @PathVariable("id") Long id) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }


    @PostMapping("/{project}")
    public ResponseEntity<AmbienteBean> save(@PathVariable("project") String project, @RequestBody AmbienteBean body) throws Exception{
        this.target = project;
        ProjectBean p = this.projectRepository.findByIdentifier(project).get();
        body.setProject(p);
        return ResponseEntity.ok(this.repository.save(body));
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{project}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }


    @GetMapping("/{project}/initial-data/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getInitialData(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        try {
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            GithubRunnerListDTO runners = service.getRunners();
            if (runners.getTotal_count() > 0) {
                resp.put("runners", runners.getRunners());
            } else {
                resp.put("runners", null);
            }
            resp.put("clients", this.getClients());
            resp.put("versions", this.versaoRepository.findAllByProjectIdentifierOrderByMajorDescMinorDescPatchDescVersionTypeAscBuildDesc(project));
            if (id > 0L) {
                resp.put("history", this.atualizacaoVersaoRepository.findAllByConclusionAndEnvironmentIdOrderByConclusionDateDesc(GithubActionConclusionEnum.success, id));
            }
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
