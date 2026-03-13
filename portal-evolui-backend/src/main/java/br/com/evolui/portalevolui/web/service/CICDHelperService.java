package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.cicd.CICDRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.EC2DTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.WorkspaceDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSRunnerConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.CICDProjectConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.CICDProjectModuleConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.AWSInstanceRunnerTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.enums.GithubRunnerLabelTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class CICDHelperService {
    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;
    @Autowired
    private CICDRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GithubVersionService githubService;

    @Autowired
    private AWSService awsService;

    @Autowired
    private VersaoRepository versaoRepository;


    @Transactional(propagation=REQUIRES_NEW)
    public Map.Entry<CICDBean, Throwable> generateVersion(CICDProjectConfigDTO config, Long userId) throws Exception {
        CICDBean bean = new CICDBean();
        try {
            if (userId == null) {
                userId = this.getLoggedUser();
            }
            List<ProjectBean> produtos = this.getProjectRepository().findAll();
            bean.setProject(produtos.stream().filter(x -> x.getId().equals(config.getProductId())).findFirst().get());
            bean.setUser(this.getUserRepository().findById(userId).get());
            bean.setRequestDate(Calendar.getInstance());

            String compileType = config.getCompileType();
            if ("stable".equals(compileType)) {
                // Buscar última versão stable e calcular próxima
                java.util.Optional<VersaoBean> lastStable = versaoRepository.findLastStableVersion(bean.getProject().getIdentifier());
                if (lastStable.isPresent()) {
                    VersaoBean last = lastStable.get();
                    String nextTag = String.format("%s.%s.%s.0", last.getMajor(), last.getMinor(), last.getPatch() + 1);
                    bean.setTag(nextTag);
                } else {
                    bean.setTag("1.0.0.0");
                }
            } else if ("patch".equals(compileType)) {
                // Buscar último build da branch e incrementar
                List<VersaoBean> branchVersions = versaoRepository.findAllByBranchAndProjectIdentifierOrderByVersionTypeAscBuildDesc(
                        config.getBranch(), bean.getProject().getIdentifier());
                if (!branchVersions.isEmpty()) {
                    VersaoBean last = branchVersions.get(0);
                    long nextBuild = Long.parseLong(last.getBuild()) + 1;
                    bean.setTag(String.format("%s.%s", last.getBranch(), nextBuild));
                } else {
                    bean.setTag(config.getBranch() + ".0");
                }
            } else {
                // Fallback para compatibilidade
                bean.setTag(config.getBranch());
                if (!StringUtils.hasText(bean.getBuild())) {
                    bean.setBuild("SNAPSHOT");
                }
            }

            return new AbstractMap.SimpleEntry<>(this.generateVersion(bean, config), null);
        } catch (Throwable e) {
            return new AbstractMap.SimpleEntry<>(bean, e);
        }
    }

    public CICDBean generateVersion(CICDBean bean, CICDProjectConfigDTO config) throws Exception {
        if (this.getRepository()
                .countByStatusNotAndBranchAndProjectId
                        (GithubActionStatusEnum.completed, bean.getBranch(), config.getProductId()) > 0) {
            throw new Exception("Já existe uma versão da mesma branch sendo testada");
        }
        String runner = this.checkRunner();
        for (CICDProjectModuleConfigDTO m : config.getModules()) {
            ProjectModuleBean modBean = bean.getProject().getModules().stream().filter(x -> x.getId().equals(m.getProductId())).findFirst().orElse(null);;
            if (modBean == null) {
                continue;
            }
            CICDModuloBean cicdModuloBean = new CICDModuloBean();
            cicdModuloBean.setProjectModule(modBean);
            cicdModuloBean.setRepositoryBranch(m.getBranch());
            cicdModuloBean.setBranch(bean.getBranch());
            cicdModuloBean.setRelativePath(modBean.getRelativePath());
            cicdModuloBean.setRepository(modBean.getRepository());

            if (!StringUtils.hasText(cicdModuloBean.getBuild())) {
                cicdModuloBean.setBuild("SNAPSHOT");
            }
            cicdModuloBean.setIncludeTests(m.getIncludeTests());
            bean.addModule(cicdModuloBean);
            if (!m.getEnabled()) {
                cicdModuloBean.setEnabled(false);
                continue;
            }

            if (modBean.isMain()) {
                cicdModuloBean.setTag(bean.getTag());
            } else {
                VersaoBuildBaseBean mBuild = new VersaoBuildBaseBean();
                mBuild.setBranch(bean.getBranch());
                mBuild.setBuild(mBuild.getDateFormatter().format(new Date()));
                cicdModuloBean.setTag(mBuild.getTag());
            }
            List<Object> lastCommitModule = null;
            if (!m.getIgnoreHashCommit()) {
                lastCommitModule = this.getRepository()
                        .findLastCommitModuleBranch(PageRequest.of(0, 1), m.getProductId(), bean.getBranch());
            }
            if (lastCommitModule != null && !lastCommitModule.isEmpty() && lastCommitModule.get(0) != null) {
                String hashCommit = lastCommitModule.get(0).toString();
                GithubBranchDTO b = this.getGithubService().getBranch(
                        cicdModuloBean.getRepository(),
                        cicdModuloBean.getRepositoryBranch(),
                        cicdModuloBean.getRelativePath());
                if (b.getCommit().getSha().equals(hashCommit)) {
                    cicdModuloBean.setEnabled(false);
                    continue;
                }
                cicdModuloBean.setEnabled(true);
            }
            else {
                cicdModuloBean.setEnabled(true);
            }
        }
        bean.setRequestDate(Calendar.getInstance());
        if (bean.getModules().stream().anyMatch(x -> x.isEnabled())) {
            bean.setHashToken(EncryptionUtil.generateToken(bean.getProject().getIdentifier()));
            String webhook = String.format("%s:%s/api/public/github/webhook-cicd/%s/%s/%s", baseUrl, port, bean.getProject().getIdentifier(), bean.getBranch(), bean.getHashToken());
            GithubCICDDTO dto = GithubCICDDTO.fromBean(bean, runner, webhook);
            System.out.println(new ObjectMapper().writeValueAsString(dto));
            bean.setStatus(GithubActionStatusEnum.queued);
            GithubWorkflowDTO workflowDTO = this.getGithubService().callCICD(bean.getProject().getRepository(), dto);
            /*
            GithubWorkflowDTO workflowDTO = new GithubWorkflowDTO();
            workflowDTO.setStatus(GithubActionStatusEnum.queued);
            workflowDTO.setId(Calendar.getInstance().getTimeInMillis());
             */
            bean.setWorkflow(workflowDTO.getId());
            bean.setStatus(workflowDTO.getStatus());
            bean.setConclusion(workflowDTO.getConclusion());
        }
        else {
            bean.setStatus(GithubActionStatusEnum.completed);
            bean.setConclusion(GithubActionConclusionEnum.skipped);
            bean.setConclusionDate(Calendar.getInstance());
        }
        return  bean;
    }

    public GithubVersionService getGithubService() {
        return githubService;
    }

    public CICDRepository getRepository() {
        return repository;
    }

    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }

    public Long getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return 1L;
        }
        UserDetailsSecurity user = (UserDetailsSecurity) auth.getPrincipal();
        return user.getId();
    }

    public void setProjectRepository(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public VersaoRepository getVersaoRepository() {
        return versaoRepository;
    }

    public AWSService getAwsService() {
        return awsService;
    }

    public void setAwsService(AWSService awsService) {
        this.awsService = awsService;
    }

    private String checkRunner() throws Exception {
        try {
            if (!this.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            if (!this.getAwsService().initialize()) {
                throw new Exception("Configuração principal aws não foi feita");
            }
            AWSRunnerConfigDTO config = this.getAwsService().getConfig().getRunnerTests();
            if (config.getInstanceType() == AWSInstanceRunnerTypeEnum.EC2) {
                EC2DTO dto = this.getAwsService().getEc2(config.getId());
                if (dto == null) {
                    throw new Exception("Runner de testes não foi encontrado na AWS");
                }
                if (dto.getInstanceState().toLowerCase().equals("running")) {
                    return this.getGithubIdentifier(config.getRunnerGithubId());
                } else if (dto.getInstanceState().toLowerCase().equals("stopped")) {
                    this.getAwsService().startEc2(dto.getId());
                    throw new Exception("A instância dedicada ao Runner estava parada. Foi enviada uma solicitação de reboot. Tente novamente em alguns minutos");
                } else {
                    throw new Exception("A instância dedicada ao Runner está com o status " + dto.getInstanceState() + " e não pode ser utilizada nem reiniciada");
                }
            } else {
                WorkspaceDTO dto = this.getAwsService().getWorspace(config.getId());
                if (dto == null) {
                    throw new Exception("Runner de versões não foi encontrado");
                }
                if (dto.getState().toLowerCase().equals("available")) {
                    return this.getGithubIdentifier(config.getRunnerGithubId());
                } else if (dto.getState().toLowerCase().equals("stopped")) {
                    getAwsService().startWorkspace(config.getId());
                    throw new Exception("A instância dedicada ao Runner estava parada. Foi enviada uma solicitação de reboot. Tente novamente em alguns minutos");
                } else {
                    throw new Exception("A instância dedicada ao Runner está com o status " + dto.getState() + " e não pode ser utilizada nem reiniciada");
                }
            }
        }
        finally {
            this.getGithubService().dispose();
            this.getAwsService().dispose();
        }

    }

    private String getGithubIdentifier(Long id) throws Exception {
        GithubRunnerDTO runner = this.getGithubService().getRunner(id);
        if (runner == null) {
            throw new Exception("Runner de testes não foi encontrado no Github");
        } else if (!runner.getStatus().equals("online")) {
            throw new Exception("Runner de testes não está online. Se a máquina foi inicializada recentemente, aguarde que o serviço seja iniciado");
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
}
