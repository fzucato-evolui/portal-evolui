package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.beans.CICDModuloBean;
import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.repository.cicd.CICDRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.CICDConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.CICDProductConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CICDService {
    private CICDConfigDTO config;
    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    CICDHelperService service;

    @Autowired
    NotificationService notificationService;
    private static Semaphore deleteSemaphore = new Semaphore(1);
    private static LinkedHashMap<Map.Entry<String, String>, Semaphore> semaphores = new LinkedHashMap<>();
    private static ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public static Semaphore getDeleteSemaphore() {
        return deleteSemaphore;
    }

    public static Semaphore getSemaphore(String target, String branch) {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(target, branch);
        if (!semaphores.containsKey(entry)) {
            semaphores.put(entry, new Semaphore(1));
        }
        return semaphores.get(entry);
    }
    @Transactional
    @Async
    public void deleteOldTests() {
        try {
            this.getConfig();
            if (this.config.getDaysForKeep() == null || this.config.getDaysForKeep().equals(0)) {
                return;
            }
            deleteSemaphore.acquire();
            Calendar limitDate = Calendar.getInstance();
            limitDate.add(Calendar.DATE, this.config.getDaysForKeep() * -1);
            List<CICDBean> beans = this.getRepository().findAllByRequestDateBeforeAndStatus(limitDate, GithubActionStatusEnum.completed);
            LinkedHashMap<Long, String> bucketKeys = new LinkedHashMap<>();
            for (CICDBean bean : beans) {
                if (bean.getWorkflow() == null || bean.getWorkflow() <= 0) {
                    continue;
                }
                bucketKeys.put(bean.getWorkflow(), bean.getProject().getIdentifier());
            }
            this.getRepository().deleteAll(beans);
            if (!bucketKeys.isEmpty()) {
                this.getAWSService().deleteCICDResults(bucketKeys);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                deleteSemaphore.release();
            } catch (Exception ex) {

            }
        }
    }
    @Transactional
    @Async
    public void searchPending(String target, String branch, String token, GithubCICDResultDTO result) {
        try {
            if (!this.getGithubService().initialize()) {
                return;
            }
            getSemaphore(target, branch).acquire();
            List<CICDBean> beans = null;
            if (!StringHelper.isEmpty(token)) {
                Optional<CICDBean> oBean = this.getRepository().findByHashTokenAndWorkflowAndStatusNot(token, result.getId(), GithubActionStatusEnum.completed);
                if (!oBean.isPresent()) {
                    return;
                }
                beans = new ArrayList<>();
                beans.add(oBean.get());
            } else {
                beans = this.getRepository().findAllByStatusNotAndBranchAndProjectIdentifier(GithubActionStatusEnum.completed, branch, target);
            }
            if (beans != null && !beans.isEmpty()) {
                for (CICDBean bean : beans) {
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
                                bean.setError(null);
                                GithubCICDResultDTO.Result dto = result.getResult();
                                if (dto != null && dto.getModules() != null && !dto.getModules().isEmpty()) {

                                    bean.setTag(dto.getTag());
                                    bean.setPureBranch(dto.getBranch());
                                    for (Map.Entry<String, GithubCICDModuleResultDTO> m : dto.getModules().entrySet()) {
                                        CICDModuloBean mBean = bean.getModules().stream().filter(x -> x.getProjectModule().getIdentifier().equals(m.getKey())).findFirst().orElse(null);
                                        if (mBean != null && mBean.isEnabled()) {
                                            //Não altera o enabled no action
                                            //mBean.setEnabled(m.getValue().getEnabled());
                                            if (StringUtils.hasText(m.getValue().getTag())) {
                                                mBean.setTag(m.getValue().getTag());
                                                mBean.setPureBranch(dto.getBranch());
                                            }
                                            mBean.setCommit(m.getValue().getCommit());
                                            mBean.setRepository(m.getValue().getRepository());
                                            mBean.setRelativePath(m.getValue().getRelativePath());
                                            mBean.setBuildSumary(m.getValue().getBuildSumary());
                                            mBean.setTestSumary(m.getValue().getTestSumary());
                                            mBean.setCheckrun(m.getValue().getCheckRunId());
                                            mBean.setFatalError(m.getValue().getFatalError());
                                            mBean.setStatus(m.getValue().getStatus());
                                        }
                                    }

                                }
                                this.getRepository().save(bean);
                                try {
                                    if (this.notificationService.initialize()) {
                                        try {
                                            this.notificationService.sendCICDAsync(bean);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                                finally {
                                    this.notificationService.dispose();
                                }
                            } else {
                                this.getRepository().save(bean);
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {

        } finally {
            this.getGithubService().dispose();
            try {
                getSemaphore(target, branch).release();
            } catch (Exception ex) {

            }
        }
    }

    private GithubCICDResultDTO getResultWorkflow(Long runId, String target) throws Exception {
        String repository = this.projectRepository.getRepositoryFromIdentifier(target);
        GithubWorkflowDTO workflow = this.getGithubService().getWorkFlow(repository, runId);

        GithubCICDResultDTO result = new GithubCICDResultDTO();
        result.setId(runId);
        result.setConclusion(workflow.getConclusion());
        if (workflow.getStatus() == GithubActionStatusEnum.completed) {

            GithubJobListDTO jobs = this.getGithubService().getWorkflowJobs(repository, runId);
            if (jobs.getTotal_count() > 0) {
                GithubJobDTO resultJob = jobs.getJobs().stream().filter(x -> x.getName().equals("result")).findFirst().get();
                String logs = this.getGithubService().getJobLogs(repository, resultJob.getId());
                String json = this.getGithubService().extractResultFromLogs(logs);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                result = mapper.readValue(json, GithubCICDResultDTO.class);

            }
            result.setConclusionDate(workflow.getUpdated_at());
        }
        result.setStatus(workflow.getStatus());
        return result;
    }

    public boolean initialize(Object... param) {
        return this.getConfig() != null;
    }

    public void refresh() {
        this.config = null;
        try {
            this.updateScheduler();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.CICD).orElse(null);
    }

    public CICDConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (CICDConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(CICDConfigDTO dto) {
        this.config = dto;
    }


    @Async
    protected void updateScheduler() throws Throwable {
        getConfig();
        try {
            scheduler.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (this.config == null || !this.config.getEnabled()) {
            return;
        }
        if (!this.config.getProducts().stream().anyMatch(x -> x.getEnabled())) {
            return;
        }

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.initialize();
        final CICDConfigDTO c = this.config;
        final Long userId = this.getLoggedUser();
        for (CICDProductConfigDTO p : c.getProducts()) {
            if (!p.getEnabled()) {
                continue;
            }
            this.createCICD(p, userId);
        }

    }

    public CICDBean createCICD(CICDProductConfigDTO p, Long userId) throws Exception {
        CronTrigger cronTrigger = null;
        final AtomicBoolean hasScheduler = new AtomicBoolean(false);
        if (!StringHelper.isEmpty(p.getCronExpression())) {
            cronTrigger = new CronTrigger(p.getCronExpression());
            hasScheduler.set(true);
        }

        if (hasScheduler.get()) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    CICDBean bean = null;
                    try {
                        Map.Entry<CICDBean, Throwable> ret = service.generateVersion(p, userId);
                        bean = ret.getKey();
                        if (ret.getValue() != null) {
                            bean.setStatus(GithubActionStatusEnum.completed);
                            bean.setConclusionDate(Calendar.getInstance());
                            bean.setConclusion(GithubActionConclusionEnum.scheduler_error);
                            bean.setError(ExceptionUtils.getStackTrace(ret.getValue()));
                        }
                        service.getRepository().save(bean);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

            };
            scheduler.schedule( r, cronTrigger);
            return null;
        }
        else {
            if (scheduler == null) {
                scheduler = new ThreadPoolTaskScheduler();
                scheduler.setPoolSize(1);
                scheduler.initialize();
            }
            else {
                boolean needsReinit = false;
                try {
                    ScheduledExecutorService executor = scheduler.getScheduledExecutor();
                    if (executor.isShutdown() || executor.isTerminated()) {
                        needsReinit = true;
                    }
                }
                catch (IllegalStateException e) {
                    needsReinit = true; // ainda não foi inicializado
                }

                if (needsReinit) {
                    scheduler = new ThreadPoolTaskScheduler();
                    scheduler.setPoolSize(1);
                    scheduler.initialize();
                }
            }
            Callable<CICDBean> r = new Callable<CICDBean>() {
                @Override
                public CICDBean call() throws Exception {
                    CICDBean bean = null;
                    try {
                        Map.Entry<CICDBean, Throwable> ret = service.generateVersion(p, userId);
                        if (ret.getValue() != null) {
                            throw ret.getValue();
                        }
                        bean = ret.getKey();
                        service.getRepository().save(bean);
                        return bean;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new Exception(e);
                    }
                }

            };
            Future<CICDBean> f = scheduler.submit(r);
            CICDBean bean = f.get();
            return bean;
        }
    }

    public GithubVersionService getGithubService() {
        return this.service.getGithubService();
    }
    public AWSService getAWSService() {
        return this.service.getAwsService();
    }

    public CICDRepository getRepository() {
        return this.service.getRepository();
    }

    public ProjectRepository getProjectRepository() {
        return this.service.getProjectRepository();
    }

    public Long getLoggedUser() {
        return this.service.getLoggedUser();
    }
}
