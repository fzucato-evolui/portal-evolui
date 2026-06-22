package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.repository.client.ClienteRepository;
import br.com.evolui.portalevolui.web.repository.metadados.MetadadosBranchRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.version.AvailableVersionDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.BranchDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    VersaoRepository versaoRepository;
    

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
        List<MetadadosBranchBean> existing =
                this.repository.findAllByBranchAndProjectIdentifier(body.getBranch(), project);
        if (existing != null && !existing.isEmpty()) {
            List<MetadadosBranchBean> others = existing.stream()
                    .filter(b -> body.getId() == null || body.getId() == 0L || !body.getId().equals(b.getId()))
                    .collect(Collectors.toList());

            Set<Long> incomingClientIds = (body.getClients() == null) ? new HashSet<>() :
                    body.getClients().stream()
                            .filter(c -> c.getClient() != null && c.getClient().getId() != null)
                            .map(c -> c.getClient().getId())
                            .collect(Collectors.toSet());

            if (incomingClientIds.isEmpty()) {
                boolean defaultExists = others.stream()
                        .anyMatch(b -> b.getClients() == null || b.getClients().isEmpty());
                if (defaultExists) {
                    throw new Exception("Já existe um registro de metadados padrão (sem clientes) para a branch "
                            + body.getBranch() + " neste projeto");
                }
            } else {
                for (MetadadosBranchBean other : others) {
                    if (other.getClients() == null) {
                        continue;
                    }
                    for (MetadadosBranchClienteBean oc : other.getClients()) {
                        if (oc.getClient() != null && oc.getClient().getId() != null
                                && incomingClientIds.contains(oc.getClient().getId())) {
                            Object cli = oc.getClient().getIdentifier() != null
                                    ? oc.getClient().getIdentifier() : oc.getClient().getId();
                            throw new Exception("Já existe um registro de metadados para a branch "
                                    + body.getBranch() + " e o cliente " + cli + " neste projeto");
                        }
                    }
                }
            }
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
        this.target = project;

        LinkedHashMap<String, Object> resp = new LinkedHashMap();
        ProjectBean projectBean = this.projectRepository.findByIdentifier(this.target).orElse(null);
        if (projectBean == null) {
            throw new Exception("Projeto não encontrado");
        }
        if (!projectBean.isLuthierProject()) {
            throw new Exception("Projeto não é um projeto Luthier");
        }

        List<VersaoBean> projectVersions = this.versaoRepository.findAllByProjectIdentifier(this.target);

        AvailableVersionDTO branches = new AvailableVersionDTO();
        Map<String, BranchDTO> seenBranches = new LinkedHashMap<>();

        if (projectVersions != null) {
            projectVersions.stream()
                    .filter(v -> v != null && org.springframework.util.StringUtils.hasText(v.getBranch()))
                    .map(VersaoBean::getBranch)
                    .distinct()
                    .sorted()
                    .forEach(branch -> {
                        BranchDTO dto = new BranchDTO();
                        dto.setVersion(branch);
                        seenBranches.put(branch, dto);
                    });
        }
        BranchDTO dtoMaster = new BranchDTO();
        dtoMaster.setVersion("master");
        seenBranches.put("master", dtoMaster);

        branches.setBranches(new ArrayList<>(seenBranches.values()));

        resp.put("branches", branches);
        resp.put("clients", this.getClients());
        return ResponseEntity.ok(resp);
    }

    private List<ClienteBean> getClients() {
        return this.clientRepository.findAllByProjectIdentifier(this.target);
    }
}
