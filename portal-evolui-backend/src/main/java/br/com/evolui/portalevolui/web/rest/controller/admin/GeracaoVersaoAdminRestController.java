package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.dto.geracao_versao.GeracaoVersaoFilterDTO;
import br.com.evolui.portalevolui.web.repository.geracao_versao.GeracaoVersaoRepository;
import br.com.evolui.portalevolui.web.repository.metadados.MetadadosBranchRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.EC2DTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.WorkspaceDTO;
import br.com.evolui.portalevolui.web.rest.dto.ax.VersionGenerationModuleRequestDTO;
import br.com.evolui.portalevolui.web.rest.dto.ax.VersionGenerationRequestDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSRunnerConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.AWSInstanceRunnerTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.enums.GithubRunnerLabelTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import br.com.evolui.portalevolui.web.rest.dto.monday.MondayUserDTO;
import br.com.evolui.portalevolui.web.rest.dto.portal_luthier.PortalLuthierContextDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.AvailableVersionDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.BranchDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.GeracaoVersaoDiffDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.GeracaoVersaoDiffModuleDTO;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.service.*;
import br.com.evolui.portalevolui.web.util.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/geracao-versao")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class GeracaoVersaoAdminRestController {

    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;
    
    private String target;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    protected GithubVersionService service;

    @Autowired
    protected AWSService awsService;

    @Autowired
    protected GeracaoVersaoSchedulerService schedulerService;

    @Autowired
    GeracaoVersaoRepository repository;

    @Autowired
    MetadadosBranchRepository metaRepository;

    @Autowired
    MondayService mondayService;

    @Autowired
    VersaoRepository versaoRepository;

    @Autowired
    private ProjectAdminRestController projectController;

    @Autowired
    private VersaoAdminRestController versaoController;

    @Autowired
    private PortalLuthierService portalLuthierService;
    
    @GetMapping("/{project}/all")
    public ResponseEntity<List<GeracaoVersaoBean>> getAll(@PathVariable("project") String project) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findAllByProjectIdentifier(project));
    }

    @GetMapping("/{project}/{id}")
    public ResponseEntity<GeracaoVersaoBean> get(@PathVariable("project") String project, @PathVariable("id") Long id) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @PostMapping("/{project}/filter")
    public ResponseEntity<LinkedHashMap> filter(@PathVariable("project") String project, @RequestBody GeracaoVersaoFilterDTO body) throws Exception{
        this.deleteOldResults();
        this.target = project;
        LinkedHashMap<String, Object> resp = new LinkedHashMap();
        long pendings = this.repository.countByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, project);
        if (pendings > 0) {
            this.searchPendingVersion();
        }
        resp.put("canGenerate", pendings == 0);
        resp.put("rows", this.repository.filter(project, body));
        return ResponseEntity.ok(resp);

    }

    @GetMapping("/{project}/can-generate")
    public ResponseEntity<Boolean> canGenerate(@PathVariable("project") String project) throws Exception{
        this.deleteOldResults();
        this.target = project;
        long pendings = this.repository.countByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, project);
        if (pendings > 0) {
            this.searchPendingVersion();
        }
        return ResponseEntity.ok(pendings == 0);

    }

    @PostMapping("/{project}")
    public ResponseEntity<GeracaoVersaoBean> generate(@PathVariable("project") String project, @RequestBody VersionGenerationRequestDTO body) throws Exception {
        this.target = project;
        return this.generateFromRequest(body);
    }

    public ResponseEntity<GeracaoVersaoBean> generateFromRequest(VersionGenerationRequestDTO body) throws Exception {
        // 1. Validações
        this.validateRequiredFields(body);
        VersaoBranchBaseBean branch = this.parseAndValidateTag(body.getTag());

        ProjectBean project = this.projectRepository.findById(body.getId()).orElse(null);
        if (project == null) {
            throw new Exception(String.format("Produto com o ID %s não foi encontrado", body.getId()));
        }
        this.projectController.validate(project);
        this.validateNoDuplicateModuleIds(body.getModules());

        // 2. Identificar módulos
        if (project.getModules() == null) {
            project.setModules(new ArrayList<>());
        }

        Set<Long> requiredIds = new HashSet<>();
        AtomicLong mainProjectId = new AtomicLong(-1L);
        project.getModules().stream()
                .forEach(m -> {
                    requiredIds.add(m.getId());
                    if (m.isMain()) {
                        mainProjectId.set(m.getId());
                    }
                });
        Set<Long> providedIds = body.getModules().stream()
                .map(VersionGenerationModuleRequestDTO::getId)
                .collect(Collectors.toSet());
        Set<Long> missing = requiredIds.stream()
                .filter(id -> !providedIds.contains(id))
                .collect(Collectors.toSet());
        boolean mainIsIncluded = providedIds.contains(mainProjectId.get());

        // 3. Carregar builds e auto-detectar compileType
        List<VersaoBean> builds = this.versaoController.getRepository()
                .findAllByBranchAndProjectIdentifierOrderByVersionTypeAscBuildDesc(branch.getBranch(), project.getIdentifier());
        VersaoBean lastAvailableVersion = builds != null && !builds.isEmpty()
                ? builds.get(0)
                : this.versaoController.getRepository().findLastStableVersion(project.getIdentifier()).orElse(null);

        if (body.getCompileType() == null) {
            if (builds == null || builds.isEmpty()) {
                body.setCompileType(CompileTypeEnum.stable);
                VersaoBean lastStable = this.versaoController.getRepository()
                        .findLastStableVersion(project.getIdentifier()).orElse(null);
                if (lastStable != null && !isBranchGreaterThan(branch, lastStable)) {
                    throw new Exception(String.format(
                            "A branch %s deve ser maior que a última versão estável (%s) do projeto %s",
                            branch.getBranch(), lastStable.getBranch(), project.getIdentifier()));
                }
            } else {
                body.setCompileType(CompileTypeEnum.patch);
            }
        }

        // 4. switch(compileType)
        VersaoBuildBaseBean build;
        switch (body.getCompileType()) {
            case patch:
                build = this.processPatchType(builds, branch, mainIsIncluded, mainProjectId.get(),
                        body, missing, project, lastAvailableVersion);
                break;
            case stable:
                build = this.processStableType(builds, branch, mainIsIncluded, mainProjectId.get(),
                        body, missing, project, lastAvailableVersion);
                break;
            case rc:
                build = this.processTransitoryType("RC",
                        EnumSet.of(CompileTypeEnum.patch, CompileTypeEnum.stable),
                        CompileTypeEnum.rc, builds, branch, mainIsIncluded, mainProjectId.get(),
                        body, missing, project, lastAvailableVersion);
                break;
            case beta:
                build = this.processTransitoryType("BETA",
                        EnumSet.of(CompileTypeEnum.patch, CompileTypeEnum.stable, CompileTypeEnum.rc),
                        CompileTypeEnum.beta, builds, branch, mainIsIncluded, mainProjectId.get(),
                        body, missing, project, lastAvailableVersion);
                break;
            case alpha:
                build = this.processTransitoryType("ALPHA",
                        EnumSet.of(CompileTypeEnum.patch, CompileTypeEnum.stable, CompileTypeEnum.rc, CompileTypeEnum.beta),
                        CompileTypeEnum.alpha, builds, branch, mainIsIncluded, mainProjectId.get(),
                        body, missing, project, lastAvailableVersion);
                break;
            default:
                throw new Exception("Tipo de compilação não suportado: " + body.getCompileType());
        }

        // 5. Montar GeracaoVersaoBean
        GeracaoVersaoBean bean = new GeracaoVersaoBean();
        bean.setProject(project);
        bean.setTag(build.getTag());
        bean.setCompileType(body.getCompileType());
        // Força inicialização lazy dos campos derivados (tag, build, branch)
        bean.getTag();
        bean.getBuild();
        bean.getBranch();

        List<GeracaoVersaoModuloBean> modules = this.buildModuleBeans(
                body, project, bean, mainProjectId.get(), mainIsIncluded, providedIds);
        bean.setModules(modules);

        // 6. Dispatch
        this.target = project.getIdentifier();
        List<GeracaoVersaoBean> pending = this.repository.findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, this.target);
        if (pending != null && !pending.isEmpty()) {
            throw new Exception("Já existe uma versão sendo gerada");
        }

        bean.setUser(this.getLoggedUser());
        this.validateMonday(bean, project.getRepository());
        String runnerIdentifier = this.checkRunner();
        bean.setRequestDate(Calendar.getInstance());
        bean.setStatus(GithubActionStatusEnum.queued);
        bean.setConclusion(null);
        bean.setHashToken(EncryptionUtil.generateToken(this.target));
        GithubWorkflowDTO workflowDTO = this.generateVersion(bean, runnerIdentifier);
        bean.setWorkflow(workflowDTO.getId());
        bean.setStatus(workflowDTO.getStatus());
        bean.setConclusion(workflowDTO.getConclusion());

        return ResponseEntity.ok(this.repository.save(bean));
    }

    @GetMapping("/{project}/cancel/{id}")
    public ResponseEntity<GeracaoVersaoBean> cancel(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = project;
            GeracaoVersaoBean bean = this.repository.findById(id).get();
            if (bean.getStatus() == GithubActionStatusEnum.completed) {
                throw new Exception("A requisição já está processada e não pode ser cancelada");
            }
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
            this.service.cancelWorkFlow(repository, bean.getWorkflow());
            bean.setConclusion(GithubActionConclusionEnum.cancelling);
            bean.setUser(this.getLoggedUser());
            this.repository.save(bean);
            return ResponseEntity.ok(this.repository.findById(id).orElse(null));
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/{project}/rerun-failed/{id}")
    public ResponseEntity<GeracaoVersaoBean> rerunFailed(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        GeracaoVersaoBean bean = this.repository.findById(id).get();
        if (bean.getStatus() != GithubActionStatusEnum.completed) {
            throw new Exception("A requisição ainda não foi processada. Espere o término e tente novamente");
        }
        if (bean.getConclusion() == GithubActionConclusionEnum.success) {
            throw new Exception("A requisição foi finalizada com sucesso. Não pode ser atualizada");
        }
        bean.setUser(this.getLoggedUser());
        this.validateMonday(bean, bean.getProject().getRepository());
        String runnerIdentifier = this.checkRunner();
        String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
        this.service.rerunFailedJobsWorkFlow(repository, bean.getWorkflow());
        bean.setRequestDate(Calendar.getInstance());
        bean.setConclusion(null);
        bean.setConclusionDate(null);
        bean.setStatus(GithubActionStatusEnum.queued);

        this.repository.save(bean);

        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @GetMapping("/{project}/logs/{id}")
    public ResponseEntity<GeracaoVersaoBean> getLogs(@PathVariable("project") String project, @PathVariable("id") Long id) {
        this.target = project;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @GetMapping("/{project}/branches")
    public ResponseEntity<AvailableVersionDTO> getBranches(@PathVariable("project") String project) throws Exception {
        try {
            this.target = project;
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            return ResponseEntity.ok(this.service.getBranches(this.projectRepository.getRepositoryFromIdentifier(this.target)));
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/branches/{moduleID}")
    public ResponseEntity<BranchesAndTagsDetailDTO> getBranchesAndTags(@PathVariable("moduleID") Long moduleID) throws Exception {
        try {
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            ProjectModuleBean module = this.projectRepository.findModuleById(moduleID).get();
            return ResponseEntity.ok(this.service.getBranchesAndTagsDetailed(module.getRepository()));
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/{project}/available-branches")
    public ResponseEntity<List<VersaoBean>> getAvailableBranches(@PathVariable("project") String project) throws Exception {
        try {
            this.target = project;
            Map<String, BranchDTO> seenBranches = new HashMap<>();
            AvailableVersionDTO versions = new AvailableVersionDTO();

            List<VersaoBean> projectVersions =
                    versaoRepository.findAllByProjectIdentifier(target);
            return ResponseEntity.ok(projectVersions);
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/{project}/link/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getLink(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = project;
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            resp.put("resp", this.service.getLinkWorkflow(repository, id));
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.dispose();
        }
    }

    @GetMapping("/{project}/monday-link/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getMondayLink(@PathVariable("project") String project, @PathVariable("id") String id) throws Exception {
        try {
            this.target = project;
            if (!this.mondayService.initialize()) {
                throw new Exception("Configuração monday não foi feita");
            }
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            resp.put("resp", this.mondayService.getLinkItem(id));
            return ResponseEntity.ok(resp);
        }
        finally {
            this.mondayService.dispose();
        }
    }

    @GetMapping("/{project}/diff/{idFrom}/{idTo}")
    public ResponseEntity<GeracaoVersaoDiffDTO> getDiff(@PathVariable("project") String project, @PathVariable("idFrom") Long idFrom, @PathVariable("idTo") Long idTo) throws Exception {
        this.target = project;
        GeracaoVersaoBean beanTo = this.repository.findById(idTo).get();
        GeracaoVersaoBean beanFrom = this.repository.findById(idFrom).get();
        GeracaoVersaoDiffDTO dto = new GeracaoVersaoDiffDTO();
        int compare = beanTo.getRequestDate().compareTo(beanFrom.getRequestDate());
        if (compare > 0) {
            dto.setTo(beanTo);
            dto.setFrom(beanFrom);
            dto.setModulesDiff(this.getModulesDiff(beanFrom, beanTo));
        } else {
            dto.setTo(beanFrom);
            dto.setFrom(beanTo);
            dto.setModulesDiff(this.getModulesDiff(beanTo, beanFrom));
        }
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{project}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("project") String project, @PathVariable("id") Long id) throws Exception {
        this.target = project;
        GeracaoVersaoBean bean = this.repository.findById(id).get();
        if (bean.getStatus() == GithubActionStatusEnum.completed || bean.getWorkflow() != null) {
            if (bean.getConclusion() == GithubActionConclusionEnum.success) {
                throw new Exception("Apenas requisições não finalizadas ou com falhas e que não tenham sido disparadas no Github é que podem ser apagadas");
            }
        }
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }

    private UserBean getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsSecurity user = (UserDetailsSecurity) auth.getPrincipal();
        return this.userRepository.findById(user.getId()).get();
    }

    private GithubWorkflowDTO generateVersion(GeracaoVersaoBean bean, String runner) throws Exception {
        String webhook = String.format("%s:%s/api/public/github/webhook-geracao-versao/%s/%s", baseUrl, port, this.target, bean.getHashToken());
        GithubGeracaoVersaoDTO dto = GithubGeracaoVersaoDTO.fromBean(bean, this.getMetadados(bean), runner, webhook);
        System.out.println(new ObjectMapper().writeValueAsString(dto));
        /*
        GithubWorkflowDTO workflow = new GithubWorkflowDTO();
        workflow.setId(Calendar.getInstance().getTimeInMillis());
        workflow.setConclusion(GithubActionConclusionEnum.neutral);
        workflow.setStatus(GithubActionStatusEnum.completed);

         */
        return this.service.callBuilder(bean.getProject().getRepository(), dto);
    }

    private void searchPendingVersion() {
        if (GeracaoVersaoSchedulerService.getSemaphore(this.target).getQueueLength() == 0) {
            this.schedulerService.searchPending(this.target, null, null);
        }
    }

    protected List<GeracaoVersaoDiffModuleDTO> getModulesDiff(GeracaoVersaoBean from, GeracaoVersaoBean to) throws Exception {
        List<GeracaoVersaoDiffModuleDTO> diffs = new ArrayList<>();
        for (GeracaoVersaoModuloBean mod : from.getModules()) {
            GeracaoVersaoModuloBean modTo = to.getModules().stream().filter(x -> x.getProjectModule().getId().equals(mod.getProjectModule().getId())).findFirst().orElse(null);
            if (modTo == null) {
                continue;
            }
            GithubDiffCommitListDTO diff = this.getDiffFromGithub(
                    mod.getProjectModule().getProject().getRepository(),
                    mod.getCommit(), modTo.getCommit());
            if (diff != null) {
                GeracaoVersaoDiffModuleDTO dto = new GeracaoVersaoDiffModuleDTO();
                dto.setModule(mod.getProjectModule());
                dto.setDiffs(diff);
                diffs.add(dto);
            }

        }
        return diffs;
    }

    protected List<MetadadosBranchBean> getMetadados(GeracaoVersaoBean bean) throws Exception {
        if (!bean.getProject().isFramework()) {
            GeracaoVersaoModuloBean modulo = bean.getModules().stream().filter(x -> x.getProjectModule().isMain()).findFirst().orElse(null);
            if (modulo.isEnabled()) {
                List<MetadadosBranchBean> meta = this.metaRepository.findAllByBranchAndProjectIdentifier(bean.getBranch(), this.target);
                if (meta == null || meta.isEmpty()) {
                    meta = this.metaRepository.findAllByBranchAndProjectIdentifier(modulo.getRepositoryBranch(), this.target);
                    if (meta != null && !meta.isEmpty()) {
                       return meta;
                    }
                }
                else {
                    return meta;
                }
                if (this.portalLuthierService.initialize()) {
                    List<PortalLuthierContextDTO> contexts = this.portalLuthierService.getContexts();
                    if (StringUtils.hasText(modulo.getRepositoryBranch()) && contexts != null && !contexts.isEmpty()) {
                        List<PortalLuthierContextDTO> branchContext = contexts.stream().filter(x ->
                                x.getRepository() != null && x.getRepository().equalsIgnoreCase(modulo.getRepository()) &&
                                        x.getBranch() != null && x.getBranch().equalsIgnoreCase(modulo.getRepositoryBranch())).collect(Collectors.toList());
                        if (branchContext.size() == 1) {
                            return Arrays.asList(branchContext.get(0).toBean(null));
                        }
//                        Long contextID = this.extractContextID(modulo);
//                        if (contextID != null) {
//                            PortalLuthierContextDTO context = contexts.stream().filter(x -> x.getId().equals(contextID)).findFirst().orElse(null);
//                            if (context != null) {
//                                return Arrays.asList(context.toBean(null));
//                            }
//                        }
                    }

                }
                throw new Exception(String.format(
                        "Metadados da Branch %s de geração nem da branch %s do módulo foram definidos. Acesse a opção Metadados Versão, duplique um existente, selecione a branch e mude o banco de dados.",
                        bean.getBranch(), modulo.getRepositoryBranch()));
            }
        }
        return null;
    }

    private GithubDiffCommitListDTO getDiffFromGithub(String repository, String commitFrom, String commitTo) throws Exception {
        try {
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            if (StringHelper.isNotEmpty(commitFrom) && StringHelper.isNotEmpty(commitTo) &&
                    !commitFrom.equals(commitTo)
            ) {
                return this.service.getDiffs(repository, commitFrom, commitTo);
            }
            return null;
        }
        finally {
            this.service.dispose();
        }
    }

    private String checkRunner() throws Exception {
        try {
            if (!this.service.initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            if (!this.awsService.initialize()) {
                throw new Exception("Configuração principal aws não foi feita");
            }
            AWSRunnerConfigDTO config = this.awsService.getConfig().getRunnerVersions();
            if (config.getInstanceType() == AWSInstanceRunnerTypeEnum.EC2) {
                EC2DTO dto = this.awsService.getEc2(config.getId());
                if (dto == null) {
                    throw new Exception("Runner de versões não foi encontrado na AWS");
                }
                if (dto.getInstanceState().toLowerCase().equals("running")) {
                    return this.getGithubIdentifier(config.getRunnerGithubId());
                } else if (dto.getInstanceState().toLowerCase().equals("stopped")) {
                    this.awsService.startEc2(dto.getId());
                    throw new Exception("A instância dedicada ao Runner estava parada. Foi enviada uma solicitação de reboot. Tente novamente em alguns minutos");
                } else {
                    throw new Exception("A instância dedicada ao Runner está com o status " + dto.getInstanceState() + " e não pode ser utilizada nem reiniciada");
                }
            } else {
                WorkspaceDTO dto = this.awsService.getWorspace(config.getId());
                if (dto == null) {
                    throw new Exception("Runner de versões não foi encontrado");
                }
                if (dto.getState().toLowerCase().equals("available")) {
                    return this.getGithubIdentifier(config.getRunnerGithubId());
                } else if (dto.getState().toLowerCase().equals("stopped")) {
                    awsService.startWorkspace(config.getId());
                    throw new Exception("A instância dedicada ao Runner estava parada. Foi enviada uma solicitação de reboot. Tente novamente em alguns minutos");
                } else {
                    throw new Exception("A instância dedicada ao Runner está com o status " + dto.getState() + " e não pode ser utilizada nem reiniciada");
                }
            }
        }
        finally {
            this.service.dispose();
            this.awsService.dispose();
        }

    }

    private String getGithubIdentifier(Long id) throws Exception {
        GithubRunnerDTO runner = this.service.getRunner(id);
        if (runner == null) {
            throw new Exception("Runner de versões não foi encontrado no Github");
        } else if (!runner.getStatus().equals("online")) {
            throw new Exception("Runner de versões não está online. Se a máquina foi inicializada recentemente, aguarde que o serviço seja iniciado");
        }
        String identifier = runner.getName();
        if (runner.getLabels() != null && !runner.getLabels().isEmpty()) {
            for (GithubRunnerLabelDTO label : runner.getLabels()) {
                if (label.getType() == GithubRunnerLabelTypeEnum.CUSTOM) {
                    identifier = label.getName();
                }
            }
        }
        return identifier;
    }

    private void validateMonday(GeracaoVersaoBean bean, String repository) throws Exception {
        try {
            if (bean.getCompileType() != CompileTypeEnum.beta) {
                if (!bean.getModules().stream().anyMatch(x -> x.getProjectModule().isMain() && x.isEnabled() && !x.getProjectModule().isFramework())) {
                    return;
                }
                if (this.mondayService.initialize(null) && this.mondayService.versionGerenarionIsEnabled()) {
                    MondayUserDTO mondayUser = this.mondayService.getUserByEmail(bean.getUser().getEmail());
                    if (mondayUser == null) {
                        throw new Exception("Você não é um usuário Monday");
                    }
                    VersaoBuildBaseBean buildBaseBean = null;
                    AvailableVersionDTO availableVersionDTO = this.service.getBranches(repository);
                    if (availableVersionDTO.getBranches() == null || availableVersionDTO.getBranches().isEmpty()) {
                        throw new Exception("Não existem branches " + bean.getBranch() + " disponíveis no github");
                    }
                    if (bean.getCompileType() == CompileTypeEnum.patch) {
                        BranchDTO branch = availableVersionDTO.getBranches().stream().filter(x -> x.getVersion().equalsIgnoreCase(bean.getBranch())).findFirst().orElse(null);
                        if (branch == null) {
                            throw new Exception("Não existe a branch " + bean.getBranch() + " disponível no github");
                        }
                        buildBaseBean = new VersaoBuildBaseBean(branch.getLastTag());
                        Integer build = Integer.parseInt(buildBaseBean.getBuild()) + 1;
                        buildBaseBean.setBuild(build.toString());

                    }
                    // Candidate
                    else {
                        BranchDTO branch = availableVersionDTO.getBranches().get(0);
                        buildBaseBean = new VersaoBuildBaseBean(branch.getVersion() + ".0");
                        buildBaseBean.setPatch(buildBaseBean.getPatch() + 1);
                        buildBaseBean = new VersaoBuildBaseBean(buildBaseBean.getMajor(), buildBaseBean.getMinor(), buildBaseBean.getPatch(), buildBaseBean.getBuild());
                    }
                    String mondayId = this.mondayService.validateVersion(buildBaseBean, this.target);
                    bean.setMondayId(mondayId);
                }
            }
        }
        finally {
            this.mondayService.dispose();
        }
    }

    private void deleteOldResults() {
        if (GeracaoVersaoSchedulerService.getDeleteSemaphore().getQueueLength() == 0) {
            this.schedulerService.deleteOldResults();
        }
    }

    // ========== Métodos auxiliares do generateFromRequest() ==========

    private void validateRequiredFields(VersionGenerationRequestDTO body) throws Exception {
        if (!StringUtils.hasText(body.getTag())) {
            throw new Exception("Campo tag é obrigatório");
        }
        if (body.getId() == null) {
            throw new Exception("ID do projeto da geração de versão é obrigatório");
        }
    }

    private VersaoBranchBaseBean parseAndValidateTag(String tag) throws Exception {
        VersaoBranchBaseBean branch = null;
        try {
            branch = new VersaoBranchBaseBean(tag);
        } catch (Exception e) {}
        if (branch == null) {
            throw new Exception("Tag informada está errada. As 2 primeiras posições devem ser numéricas e a terceira numérica + qualifiter opcional. Ex: 10.0.1-rc3");
        }
        return branch;
    }

    private boolean isBranchGreaterThan(VersaoBranchBaseBean a, VersaoBranchBaseBean b) {
        if (a.getMajor() != b.getMajor()) return a.getMajor() > b.getMajor();
        if (a.getMinor() != b.getMinor()) return a.getMinor() > b.getMinor();
        return a.getPatch() > b.getPatch();
    }

    private void validateNoDuplicateModuleIds(List<VersionGenerationModuleRequestDTO> modules) throws Exception {
        Set<Long> seenIds = new HashSet<>();
        boolean hasRepeated = modules.stream()
                .map(VersionGenerationModuleRequestDTO::getId)
                .anyMatch(id -> !seenIds.add(id));
        if (hasRepeated) {
            throw new Exception("Existem módulos com ids repetidos");
        }
    }

    private void assignModuleTags(
            List<VersionGenerationModuleRequestDTO> modules,
            long mainProjectId,
            VersaoBuildBaseBean mainBuild,
            String branchStr) {
        modules.forEach(m -> {
            if (m.getId().equals(mainProjectId)) {
                m.setTag(mainBuild.getTag());
            } else {
                VersaoBuildBaseBean mBuild = new VersaoBuildBaseBean();
                mBuild.setBranch(branchStr);
                mBuild.setQualifier(mainBuild.getQualifier());
                mBuild.setBuild(mainBuild.getDateFormatter().format(new Date()));
                m.setTag(mBuild.getTag());
            }
        });
    }

    private void fillMissingModules(
            Set<Long> missing,
            ProjectBean project,
            VersionGenerationRequestDTO body,
            VersaoBean sourceVersion,
            CompileTypeEnum compileType) throws Exception {
        if (missing.isEmpty()) {
            return;
        }
        if (sourceVersion == null) {
            throw new Exception("Não é possível obter a última build disponível para os módulos faltantes");
        }
        for (ProjectModuleBean m : project.getModules()) {
            if ((!m.isMain() && m.isFramework()) || !missing.contains(m.getId())) {
                continue;
            }
            VersaoModuloBean mVersao = sourceVersion.getModules().stream()
                    .filter(vModule -> vModule.getProjectModule().getId().equals(m.getId()))
                    .findFirst().orElse(null);
            if (mVersao == null) {
                throw new Exception(String.format(
                        "Não é possível obter a última build disponível em %s para o módulo faltante %s",
                        compileType.value(), m.getIdentifier()));
            }
            VersionGenerationModuleRequestDTO missingModule = new VersionGenerationModuleRequestDTO();
            missingModule.setId(m.getId());
            missingModule.setTag(mVersao.getTag());
            missingModule.setEnabled(false);
            body.getModules().add(missingModule);
        }
    }

    private VersaoBuildBaseBean processPatchType(
            List<VersaoBean> builds,
            VersaoBranchBaseBean branch,
            boolean mainIsIncluded,
            long mainProjectId,
            VersionGenerationRequestDTO body,
            Set<Long> missing,
            ProjectBean project,
            VersaoBean lastAvailableVersion) throws Exception {

        if (builds == null || builds.isEmpty() || CompileTypeEnum.isTransitoryType(builds.get(0).getVersionType())) {
            throw new Exception(String.format("Não existe nenhuma build para o projeto %s de versão %s",
                    project.getIdentifier(), branch.getBranch()));
        }
        VersaoBuildBaseBean build = new VersaoBuildBaseBean();
        build.setTag(builds.get(0).getTag());
        if (mainIsIncluded) {
            build.setBuild("" + (Long.parseLong(build.getBuild()) + 1L));
        }
        this.assignModuleTags(body.getModules(), mainProjectId, build, branch.getBranch());
        this.fillMissingModules(missing, project, body, lastAvailableVersion, CompileTypeEnum.patch);
        return build;
    }

    private VersaoBuildBaseBean processStableType(
            List<VersaoBean> builds,
            VersaoBranchBaseBean branch,
            boolean mainIsIncluded,
            long mainProjectId,
            VersionGenerationRequestDTO body,
            Set<Long> missing,
            ProjectBean project,
            VersaoBean lastAvailableVersion) throws Exception {

        if (builds != null && !builds.isEmpty() && !CompileTypeEnum.isTransitoryType(builds.get(0).getVersionType())) {
            throw new Exception(String.format("Já existe uma build para o projeto %s de versão %s",
                    project.getIdentifier(), branch.getBranch()));
        }
        if (!mainIsIncluded) {
            throw new Exception(String.format("Em builds do tipo %s o módulo principal precisa estar incluído",
                    CompileTypeEnum.stable.value()));
        }

        VersaoBuildBaseBean build = new VersaoBuildBaseBean();
        build.setBranch(branch.getBranch());
        build.setBuild("0");
        this.assignModuleTags(body.getModules(), mainProjectId, build, branch.getBranch());
        this.fillMissingModules(missing, project, body, lastAvailableVersion, CompileTypeEnum.stable);

        return build;
    }

    private VersaoBuildBaseBean processTransitoryType(
            String qualifier,
            Set<CompileTypeEnum> blockedHigherTypes,
            CompileTypeEnum compileType,
            List<VersaoBean> builds,
            VersaoBranchBaseBean branch,
            boolean mainIsIncluded,
            long mainProjectId,
            VersionGenerationRequestDTO body,
            Set<Long> missing,
            ProjectBean project,
            VersaoBean lastAvailableVersion) throws Exception {

        VersaoBuildBaseBean build = new VersaoBuildBaseBean();
        CompileTypeEnum lastBuildCompileType = null;

        if (builds != null && !builds.isEmpty()) {
            lastBuildCompileType = builds.get(0).getVersionType();
            if (lastBuildCompileType == CompileTypeEnum.patch || lastBuildCompileType == CompileTypeEnum.stable) {
                throw new Exception(String.format("Já existe uma build para o projeto %s de versão %s",
                        project.getIdentifier(), branch.getBranch()));
            }
            Set<CompileTypeEnum> additionalBlocked = new HashSet<>(blockedHigherTypes);
            additionalBlocked.remove(CompileTypeEnum.patch);
            additionalBlocked.remove(CompileTypeEnum.stable);
            if (additionalBlocked.contains(lastBuildCompileType)) {
                throw new Exception(String.format("Já existe uma build para o projeto %s de versão %s do tipo %s",
                        project.getIdentifier(), branch.getBranch(), lastBuildCompileType.value()));
            }
        }

        if (!mainIsIncluded) {
            if (lastBuildCompileType == null || lastBuildCompileType != compileType) {
                throw new Exception(String.format("Em builds do tipo %s, é preciso que tenha sido gerada " +
                        "anteriormente uma compilação do tipo %s para o módulo principal",
                        compileType.value(), compileType.value()));
            }
            VersaoModuloBean mMainVersao = builds.get(0).getModules().stream()
                    .filter(vModule -> vModule.getProjectModule().getId().equals(mainProjectId))
                    .findFirst().orElse(null);
            if (mMainVersao == null) {
                throw new Exception(String.format("Não é possível obter a última build %s disponível para o módulo principal",
                        compileType.value()));
            }
            build.setTag(mMainVersao.getTag());
            this.assignModuleTags(body.getModules(), mainProjectId, build, branch.getBranch());
            this.fillMissingModules(missing, project, body, builds.get(0), compileType);
        } else {
            build.setBranch(branch.getBranch());
            build.setQualifier(qualifier);
            build.setBuild(build.getDateFormatter().format(new Date()));
            this.assignModuleTags(body.getModules(), mainProjectId, build, branch.getBranch());
            this.fillMissingModules(missing, project, body, lastAvailableVersion, compileType);
        }

        return build;
    }

    private List<GeracaoVersaoModuloBean> buildModuleBeans(
            VersionGenerationRequestDTO body,
            ProjectBean project,
            GeracaoVersaoBean bean,
            long mainProjectId,
            boolean mainIsIncluded,
            Set<Long> providedIds) throws Exception {

        List<GeracaoVersaoModuloBean> modules = new ArrayList<>();

        for (VersionGenerationModuleRequestDTO module : body.getModules()) {
            GeracaoVersaoModuloBean moduleBean = new GeracaoVersaoModuloBean();
            if (!StringUtils.hasText(module.getTag())) {
                throw new Exception("Campo tag deve ser preenchido nos módulos de geração");
            }
            if (module.getId() == null) {
                throw new Exception("ID dos módulos incluídos na geração de versão é obrigatório");
            }

            moduleBean.setTag(module.getTag());
            moduleBean.setRepositoryBranch(module.getBranch());
            moduleBean.setCommit(module.getCommit());

            ProjectModuleBean projectModule = project.getModules().stream()
                    .filter(m -> m.getId().equals(module.getId()))
                    .findFirst().orElse(null);

            if (projectModule == null) {
                throw new Exception(String.format("Produto/Módulo com o identificador %s não pertence ao escopo do projeto %s",
                        module.getId(), project.getIdentifier()));
            }
            if (!projectModule.isMain() && projectModule.isFramework()) {
                continue;
            }
            if (projectModule.isMain()) {
                if (!moduleBean.getBranch().equals(bean.getBranch())) {
                    throw new Exception("Versão do módulo principal deve ser a mesma da versão do projeto da Geração de Versão");
                }
            }
            moduleBean.setRelativePath(projectModule.getRelativePath());
            moduleBean.setRepository(projectModule.getRepository());
            moduleBean.setProjectModule(projectModule);
            moduleBean.setEnabled(true);
            modules.add(moduleBean);

            if (!module.isEnabled()) {
                moduleBean.setEnabled(false);
                continue;
            }

            if (!StringUtils.hasText(module.getBranch()) && !StringUtils.hasText(module.getCommit())) {
                throw new Exception("Ou a branch ou o hashCommit devem ser preenchidos nos módulos de geração");
            }

            if (projectModule.getBond() != null) {
                Long bondId = projectModule.getBond().getId();
                VersionGenerationModuleRequestDTO moduleBond = body.getModules().stream()
                        .filter(m -> m.getId().equals(bondId))
                        .findFirst().orElse(null);
                if (moduleBond == null || !moduleBond.isEnabled()) {
                    throw new Exception(
                            String.format("Quando módulos com vínculos de compilação como o %s, que tem vínculo com o %s, forem incluídos na compilação, " +
                                    "seu módulo 'pai' também deve ser incluído", projectModule.getIdentifier(), projectModule.getBond().getIdentifier()));
                }
                if (!Objects.equals(module.getBranch(), moduleBond.getBranch()) || !Objects.equals(module.getCommit(), moduleBond.getCommit())) {
                    throw new Exception(
                            String.format("Módulos vinculados devem ter os mesmos valores de branch e commit que seu módulo pai. " +
                                    "O módulo %s (bond de %s) possui valores divergentes", projectModule.getIdentifier(), projectModule.getBond().getIdentifier()));
                }
            }
        }

        for (ProjectModuleBean module : project.getModules()) {
            if (!providedIds.contains(module.getId()) && !module.isMain() && module.isFramework()) {
                GeracaoVersaoModuloBean moduleBean = new GeracaoVersaoModuloBean();
                moduleBean.setProjectModule(module);
                moduleBean.setEnabled(mainIsIncluded);
                moduleBean.setTag("SNAPSHOT");
                modules.add(moduleBean);
            }
        }

        return modules;
    }

    public LinkedHashMap<String, Long> extractContextsIDs(GeracaoVersaoModuloBean module) throws Exception {
        String yaml = this.service.getFileContent(module.getRepository(), module.getRepositoryBranch(), ".github/workflows/import-metadata-qa.yml");

        if (!StringUtils.hasText(yaml)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(yaml);

        JsonNode envNode = root
                .path("jobs")
                .path("import-metadata")
                .path("env");

        LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        if (envNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = envNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                result.put(entry.getKey(), entry.getValue().asLong());
            }
        }

        return result;
    }

    public Long extractContextID(GeracaoVersaoModuloBean module) throws Exception {
        LinkedHashMap<String, Long> envs = extractContextsIDs(module);
        if (envs == null) {
            return null;
        }
        else {
            if (module.getRepositoryBranch().toLowerCase().contains("hotfix")) {
                return envs.get("LUTHIER_DATABASE_HOTFIX_QA");
            }
            return envs.get("LUTHIER_DATABASE_QA");
        }
    }
}
