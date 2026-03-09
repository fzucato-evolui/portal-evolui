package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.AmbienteBean;
import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.VersaoBean;
import br.com.evolui.portalevolui.web.repository.ambiente.AmbienteRepository;
import br.com.evolui.portalevolui.web.repository.metadados.MetadadosBranchRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.service.AWSService;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
@RequestMapping("/api/admin/versao")
public class VersaoAdminRestController {

    private String target;
    @Autowired
    private
    VersaoRepository repository;
    @Autowired
    MetadadosBranchRepository metadadosRepository;

    @Autowired
    AmbienteRepository ambienteRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    AWSService awsService;

    @Autowired
    GithubVersionService githubService;

    @GetMapping("/{project}/all")
    public ResponseEntity<List<VersaoBean>> getAll(@PathVariable("project") String project) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findAllByProjectIdentifierOrderByMajorDescMinorDescPatchDescVersionTypeAscBuildDesc(project));
    }

    @GetMapping("/{project}/{id}")
    public ResponseEntity<VersaoBean> get(@PathVariable("project") String project, @PathVariable("id") Long id) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @PostMapping("/{project}")
    public ResponseEntity<VersaoBean> save(@PathVariable("project") String project, @RequestBody VersaoBean body) throws Exception{
        this.target = project;
        return ResponseEntity.ok(this.repository.save(body));
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{project}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{project}/branch/{branch}")
    @Transactional
    public ResponseEntity<LinkedHashMap<String, List<String>>> delete(@PathVariable("project") String project, @PathVariable("branch") String branch) throws Exception {
        try {
            this.target = project;
            ProjectBean p = this.projectRepository.findByIdentifier(project).get();
            if (!this.awsService.initialize()) {
                throw new Exception("Configuração principal aws não foi feita");
            }
            if (!this.githubService.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            List<String> errors = new ArrayList<>();
            List<MetadadosBranchBean> metas = this.metadadosRepository.findAllByBranchAndProjectIdentifier(branch, project);
            if (metas != null && !metas.isEmpty()) {
                errors.add(String.format("Existem configurações de metadados do %s que ainda usam a branch %s", project, branch));
            }
            List<AmbienteBean> ambientes = this.ambienteRepository.findAllByBranchAndProjectIdentifier(branch, project);
            if (ambientes != null && !ambientes.isEmpty()) {
                errors.add(String.format("Existem ambientes do %s que ainda usam a branch %s", project, branch));
            }
            if (!errors.isEmpty()) {
                throw new Exception(String.join("\r\n", errors));
            }

            this.repository.deleteAllByBranchAndProjectIdentifier(branch, project);

            //Delete bucket
            try {
                this.awsService.deleteVersionBucketFolder(project, branch);
            } catch (Exception ex) {
                errors.add("Erro ao remover pasta do bucket: " + ex.getMessage());
            }

            LinkedHashMap<String, List<String>> resp = new LinkedHashMap();
            resp.put("alerts", errors);
            return ResponseEntity.ok(resp);
        }
        finally {
            this.awsService.dispose();
            this.githubService.dispose();
        }
    }

    public VersaoRepository getRepository() {
        return repository;
    }
}
