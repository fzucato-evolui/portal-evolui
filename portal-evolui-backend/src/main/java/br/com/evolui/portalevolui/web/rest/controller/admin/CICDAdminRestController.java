package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.ProjectModuleBean;
import br.com.evolui.portalevolui.web.beans.VersaoBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.repository.dto.cicd.CICDFilterDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.CICDProjectConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubBranchDTO;
import br.com.evolui.portalevolui.web.service.CICDService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/cicd")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class CICDAdminRestController {

    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;
    
    private String target;

    @Autowired
    protected CICDService service;
    
    
    @GetMapping("/{produto}/all")
    public ResponseEntity<List<CICDBean>> getAll(@PathVariable("produto") String produto) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findAllByProjectIdentifier(produto));
    }

    @GetMapping("/{produto}/{id}")
    public ResponseEntity<CICDBean> get(@PathVariable("produto") String produto, @PathVariable("id") Long id) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
    }

    @PostMapping("/{produto}/filter")
    public ResponseEntity<List<CICDBean>> filter(@PathVariable("produto") String produto, @RequestBody CICDFilterDTO body) throws Exception{
        this.target = produto;
        List<CICDBean> beans = this.service.getRepository().findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, produto);
        if (beans != null && !beans.isEmpty()) {
            for (CICDBean bean : beans) {
                this.searchPendingVersion(bean.getBranch());
            }
        }
        else {
            this.deleteOldTests();
        }

        return ResponseEntity.ok(this.service.getRepository().filter(produto, body));

    }

    @PostMapping("/{produto}/run")
    public ResponseEntity<CICDBean> run(@PathVariable("produto") String produto, @RequestBody CICDProjectConfigDTO body) throws Exception{
        this.target = produto;
        List<CICDBean> beans = this.service.getRepository().findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, produto);
        if (beans != null && !beans.isEmpty()) {
            for (CICDBean bean : beans) {
                this.searchPendingVersion(bean.getBranch());
            }
        }
        else {
            this.deleteOldTests();
        }
        CICDBean bean = this.service.createCICD(body, null);
        return ResponseEntity.ok(bean);
    }

    @GetMapping("/{produto}/logs/{id}")
    public ResponseEntity<CICDBean> getLogs(@PathVariable("produto") String produto, @PathVariable("id") Long id) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
    }

    @GetMapping("/{produto}/branches")
    public ResponseEntity<List<String>> getBranches(@PathVariable("produto") String produto) throws Exception {
        try {
            this.target = produto;
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            ProjectBean p = this.service.getProjectRepository().findByIdentifier(this.target).get();
            List<GithubBranchDTO> bs = this.service.getGithubService().getAllBranches(p.getRepository());
            return ResponseEntity.ok(bs.stream().map(x -> x.getName()).collect(Collectors.toList()));
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/versions")
    public ResponseEntity<List<VersaoBean>> getVersions(@PathVariable("produto") String produto) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getVersaoRepository()
                .findAllByProjectIdentifierOrderByMajorDescMinorDescPatchDescVersionTypeAscBuildDesc(produto));
    }

    @GetMapping("/module-branches/{moduleId}")
    public ResponseEntity<List<String>> getModuleBranches(@PathVariable("moduleId") Long moduleId) throws Exception {
        try {
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            ProjectModuleBean module = this.service.getProjectRepository().findModuleById(moduleId)
                    .orElseThrow(() -> new Exception("Módulo não encontrado"));
            String repository = module.getRepository();
            List<GithubBranchDTO> bs = this.service.getGithubService().getAllBranches(repository);
            return ResponseEntity.ok(bs.stream().map(x -> x.getName()).collect(Collectors.toList()));
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/link/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getLink(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = produto;
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            String repository = this.service.getProjectRepository().getRepositoryFromIdentifier(this.target);
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            resp.put("resp", this.service.getGithubService().getLinkWorkflow(repository, id));
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/detailed-report/{id}")
    public ResponseEntity<JsonNode> getDetailedReport(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = produto;
            if (!this.service.getAWSService().initialize()) {
                throw new Exception("Configuração principal aws não foi feita");
            }
            return ResponseEntity.ok(new ObjectMapper().readTree(new URL(this.service.getAWSService().getLinkCICDReport(this.target, id))));
        }
        finally {
            this.service.getAWSService().dispose();
        }
    }

    @GetMapping("/{produto}/checkrun-link/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getCheckrunLink(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            ProjectBean projectBean = this.service.getProjectRepository().findByIdentifier(this.target).get();
            resp.put("resp", this.service.getGithubService().getLinkCheckrun(projectBean.getRepository(), id));
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/errors/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getErrors(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        this.target = produto;
        CICDBean bean = this.service.getRepository().findById(id).get();
        LinkedHashMap<String, Object> resp = new LinkedHashMap();
        resp.put("resp", bean.getError());
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{produto}/{id}")
    @Transactional
    public ResponseEntity<LinkedHashMap<String, List<String>>> delete(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = produto;

            if (!this.service.getAWSService().initialize()) {
                throw new Exception("Configuração principal aws não foi feita");
            }
            List<String> errors = new ArrayList<>();

            CICDBean bean = this.service.getRepository().findById(id).get();
            if (bean.getStatus() != GithubActionStatusEnum.completed) {
                throw new Exception("Integração ainda não foi finalizada");
            }
            Long workFlow = bean.getWorkflow();
            this.service.getRepository().deleteById(id);

            if (workFlow != null) {
                LinkedHashMap<Long, String> productKeys = new LinkedHashMap<>();
                productKeys.put(workFlow, produto);
                //Delete bucket
                try {
                    this.service.getAWSService().deleteCICDResults(productKeys);
                } catch (Exception ex) {
                    errors.add("Erro ao remover pasta do bucket: " + ex.getMessage());
                }
            }

            LinkedHashMap<String, List<String>> resp = new LinkedHashMap();
            resp.put("alerts", errors);
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.refresh();
        }
    }


    private void searchPendingVersion(String branch) {
        if (CICDService.getSemaphore(this.target, branch).getQueueLength() == 0) {
            this.service.searchPending(this.target, branch, null, null);
        }
    }

    private void deleteOldTests() {
        if (CICDService.getDeleteSemaphore().getQueueLength() == 0) {
            this.service.deleteOldTests();
        }
    }


}
