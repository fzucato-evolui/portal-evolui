package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.GeracaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.VersaoBean;
import br.com.evolui.portalevolui.web.beans.VersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.geracao_versao.GeracaoVersaoRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.GithubConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class GeracaoVersaoSchedulerService {
    @Autowired
    GithubVersionService service;
    @Autowired
    GeracaoVersaoRepository repository;
    @Autowired
    VersaoRepository versaoRepository;
    @Autowired
    NotificationService notificationService;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    AXService axService;

    private static Semaphore deleteSemaphore = new Semaphore(1);

    private static LinkedHashMap<String, Semaphore> semaphores = new LinkedHashMap<>();

    public static Semaphore getSemaphore(String target) {
        if (!semaphores.containsKey(target)) {
            semaphores.put(target, new Semaphore(1));
        }
        return semaphores.get(target);
    }

    public static Semaphore getDeleteSemaphore() {
        return deleteSemaphore;
    }

    //@Scheduled(cron = "0 */2 * * * *")
    @Transactional
    @Async
    public void searchPending(String target, String token, GithubGeracaoVersaoResultDTO result) {
        try {
            if (!this.service.initialize()) {
                return;
            }
            getSemaphore(target).acquire();
            List<GeracaoVersaoBean> beans = null;
            if (!StringHelper.isEmpty(token)) {
                Optional<GeracaoVersaoBean> oBean = this.repository.findByHashTokenAndWorkflowAndStatusNot(token, result.getId(), GithubActionStatusEnum.completed);
                if (!oBean.isPresent()) {
                    return;
                }
                beans = new ArrayList<>();
                beans.add(oBean.get());
            } else {
                beans = this.repository.findAllByStatusNotAndProjectIdentifier(GithubActionStatusEnum.completed, target);
            }
            if (beans != null && !beans.isEmpty()) {
                for (GeracaoVersaoBean bean : beans) {
                    try {
                        if (result == null) {
                            long diff = Calendar.getInstance().getTimeInMillis() - bean.getRequestDate().getTimeInMillis();
                            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                            if (minutes < 10) {
                                continue;
                            }
                            result = this.getResultWorkflow(bean.getWorkflow(), target);
                        }
                        if (result != null && bean.getStatus() != result.getStatus()) {
                            bean.setStatus(result.getStatus());
                            if (result.getStatus() == GithubActionStatusEnum.completed) {
                                bean.setConclusionDate(result.getConclusionDate());
                                bean.setConclusion(result.getConclusion());

                                GithubGeracaoVersaoResultDTO.Result dto = result.getResult();
                                if (dto != null && result.getConclusion() == GithubActionConclusionEnum.success && dto.getModules() != null && !dto.getModules().isEmpty()) {

                                    bean.setTag(dto.getTag());
                                    for (Map.Entry<String, GithubGeracaoVersaoModuleResultDTO> m : dto.getModules().entrySet()) {
                                        GeracaoVersaoModuloBean mBean = bean.getModules().stream().filter(x -> x.getProjectModule().getIdentifier().equals(m.getKey())).findFirst().orElse(null);
                                        if (mBean != null) {
                                            mBean.setEnabled(m.getValue().getEnabled());
                                            if (StringUtils.hasText(m.getValue().getTag())) {
                                                mBean.setTag(m.getValue().getTag());
                                            }
                                            mBean.setCommit(m.getValue().getCommit());
                                            mBean.setRepository(m.getValue().getRepository());
                                            mBean.setRelativePath(m.getValue().getRelativePath());
                                        }
                                    }

                                    if (!StringHelper.isEmpty(bean.getTag())) {
                                        VersaoBean vBean = this.versaoRepository.findByTagAndProjectIdentifier(bean.getTag(), target).orElse(null);
                                        if(vBean == null) {
                                            vBean = new VersaoBean();
                                            vBean.setTag(bean.getTag());
                                            vBean.setProject(bean.getProject());
                                            vBean.setModules(new ArrayList<>());
                                            vBean.setVersionType(bean.getCompileType());
                                        }

                                        for (GeracaoVersaoModuloBean v : bean.getModules()) {
                                            VersaoModuloBean vModuloBean = vBean.getModules().stream().filter(x -> x.getProjectModule() != null && x.getProjectModule().getId().equals(v.getProjectModule().getId())).findFirst().orElse(null);
                                            if (vModuloBean == null) {
                                                vModuloBean = new VersaoModuloBean();
                                                vModuloBean.setProjectModule(v.getProjectModule());
                                                vBean.addModule(vModuloBean);
                                            }
                                            vModuloBean.setTag(v.getTag());
                                            vModuloBean.setCommit(v.getCommit());
                                            vModuloBean.setRepository(v.getRepository());
                                            vModuloBean.setRelativePath(v.getRelativePath());
                                        }
                                        this.versaoRepository.save(vBean);
                                    }
                                }
                                this.repository.save(bean);
                                if (bean.getCompileType() == CompileTypeEnum.stable && bean.getConclusion() == GithubActionConclusionEnum.success) {
                                    try {
                                        if (!this.axService.initialize()) {
                                            String branchName = bean.getBranch();
                                            Map<String, String> repoHashCommit = new LinkedHashMap<>();
                                            for (GeracaoVersaoModuloBean moduloBean : bean.getModules()) {
                                                if (moduloBean.isEnabled() && StringUtils.hasText(moduloBean.getCommit()) && StringUtils.hasText(moduloBean.getRepository())) {
                                                    repoHashCommit.putIfAbsent(moduloBean.getRepository(), moduloBean.getCommit());
                                                }
                                            }
                                            if (repoHashCommit != null && !repoHashCommit.isEmpty()) {
                                                try {
                                                    this.service.createBranchesAsync(branchName, repoHashCommit);
                                                } catch (Exception ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                try {
                                    if (this.notificationService.initialize()) {
                                        try {
                                            this.notificationService.sendVersionAsync(bean);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                                finally {
                                    this.notificationService.dispose();
                                }
                            } else {
                                this.repository.save(bean);
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {

        } finally {
            this.service.dispose();
            try {
                getSemaphore(target).release();
            } catch (Exception ex) {

            }
        }
    }

    private GithubGeracaoVersaoResultDTO getResultWorkflow(Long runId, String target) throws Exception {
        String repository = this.projectRepository.getRepositoryFromIdentifier(target);
        GithubWorkflowDTO workflow = this.service.getWorkFlow(repository, runId);

        GithubGeracaoVersaoResultDTO result = new GithubGeracaoVersaoResultDTO();
        result.setId(runId);
        result.setConclusion(workflow.getConclusion());
        if (workflow.getStatus() == GithubActionStatusEnum.completed) {

            GithubJobListDTO jobs = this.service.getWorkflowJobs(repository, runId);
            if (jobs.getTotal_count() > 0) {
                GithubJobDTO resultJob = jobs.getJobs().stream().filter(x -> x.getName().equals("result")).findFirst().get();
                String logs = this.service.getJobLogs(repository, resultJob.getId());
                String json = this.service.extractResultFromLogs(logs);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                result = mapper.readValue(json, GithubGeracaoVersaoResultDTO.class);

            }
            result.setConclusionDate(workflow.getUpdated_at());
        }
        result.setStatus(workflow.getStatus());
        return result;
    }

    @Transactional
    @Async
    public void deleteOldResults() {
        try {
            GithubConfigDTO config = this.service.getConfig();
            if (config.getDaysForKeep() == null || config.getDaysForKeep().equals(0)) {
                return;
            }
            deleteSemaphore.acquire();
            Calendar limitDate = Calendar.getInstance();
            limitDate.add(Calendar.DATE, config.getDaysForKeep() * -1);
            List<GeracaoVersaoBean> beans = this.repository.findAllByRequestDateBeforeAndStatus(limitDate, GithubActionStatusEnum.completed);
            this.repository.deleteAll(beans);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                deleteSemaphore.release();
            } catch (Exception ex) {

            }
        }
    }
}
