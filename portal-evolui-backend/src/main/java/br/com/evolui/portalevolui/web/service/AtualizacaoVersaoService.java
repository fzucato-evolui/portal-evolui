package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.AmbienteBean;
import br.com.evolui.portalevolui.web.beans.AmbienteModuloBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.ambiente.AmbienteRepository;
import br.com.evolui.portalevolui.web.repository.atualizacao_versao.AtualizacaoVersaoRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.GithubConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class AtualizacaoVersaoService {

    @Autowired
    NotificationService notificationService;

    @Autowired
    AtualizacaoVersaoHelperService service;

    @Autowired
    ProjectRepository projectRepository;

    private static Semaphore deleteSemaphore = new Semaphore(1);
    private static LinkedHashMap<String, Semaphore> semaphores = new LinkedHashMap<>();
    private static LinkedHashMap<Long, ThreadPoolTaskScheduler> schedulers = new LinkedHashMap<>();

    public static Semaphore getSemaphore(String target) {
        if (!semaphores.containsKey(target)) {
            semaphores.put(target, new Semaphore(1));
        }
        return semaphores.get(target);
    }
    public static ThreadPoolTaskScheduler getScheduler(Long id) {

        return schedulers.get(id);
    }

    public static void cancelSchedule(Long id) {
        ThreadPoolTaskScheduler scheduler = getScheduler(id);
        if (scheduler != null) {
            scheduler.destroy();
            schedulers.remove(id);
        }
    }

    public static Semaphore getDeleteSemaphore() {
        return deleteSemaphore;
    }

    //@Scheduled(cron = "0 */2 * * * *")
    @Transactional
    @Async
    public void searchPending(String target, String token, GithubGeracaoVersaoResultDTO result) {
        try {
            if (!this.getGithubService().initialize()) {
                return;
            }
            getSemaphore(target).acquire();
            String repository = this.projectRepository.getRepositoryFromIdentifier(target);
            List<AtualizacaoVersaoBean> beans = null;
            if (!StringHelper.isEmpty(token)) {
                Optional<AtualizacaoVersaoBean> oBean = this.getRepository().findByHashTokenAndWorkflowAndStatusNot(token, result.getId(), GithubActionStatusEnum.completed);
                if (!oBean.isPresent()) {
                    return;
                }
                beans = new ArrayList<>();
                beans.add(oBean.get());
            } else {
                beans = this.getRepository().findAllByStatusNotAndEnvironmentProjectIdentifierAndWorkflowIsNotNull(GithubActionStatusEnum.completed, target);
            }
            if (beans != null && !beans.isEmpty()) {
                for (AtualizacaoVersaoBean bean : beans) {
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
                                GithubGeracaoVersaoResultDTO.Result dto = result.getResult();
                                if (dto != null &&
                                        (result.getConclusion() == GithubActionConclusionEnum.success || result.getConclusion() == GithubActionConclusionEnum.warning) &&
                                        dto.getModules() != null && !dto.getModules().isEmpty()) {

                                    bean.setTag(dto.getTag());
                                    for (Map.Entry<String, GithubGeracaoVersaoModuleResultDTO> m : dto.getModules().entrySet()) {
                                        AtualizacaoVersaoModuloBean mBean = bean.getModules().stream().filter(x -> x.getEnvironmentModule().getProjectModule().getIdentifier().equals(m.getKey())).findFirst().orElse(null);
                                        if (mBean != null) {
                                            //Não altera o enabled no action
                                            //mBean.setEnabled(m.getValue().getEnabled());
                                            mBean.setTag(m.getValue().getTag());
                                            mBean.setCommit(m.getValue().getCommit());
                                            mBean.setRepository(m.getValue().getRepository());
                                            mBean.setRelativePath(m.getValue().getRelativePath());
                                        }
                                    }

                                    if (!StringHelper.isEmpty(bean.getTag())) {
                                        AmbienteBean vBean = this.getAmbienteRepository().findById(bean.getEnvironment().getId()).get();
                                        vBean.setTag(bean.getTag());

                                        for (AtualizacaoVersaoModuloBean v : bean.getModules()) {
                                            if (v.isEnabled()) {
                                                AmbienteModuloBean vModuloBean = v.getEnvironmentModule();
                                                if (vModuloBean != null) {
                                                    vModuloBean.setTag(v.getTag());
                                                    vModuloBean.setCommit(v.getCommit());
                                                    vModuloBean.setRepository(v.getRepository());
                                                    vModuloBean.setRelativePath(v.getRelativePath());
                                                }
                                            }

                                        }
                                        this.getAmbienteRepository().save(vBean);
                                    }
                                }
                                this.getRepository().save(bean);
                                try {
                                    if (this.notificationService.initialize()) {
                                        try {
                                            this.notificationService.sendUpdateVersionAsync(bean);
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
                getSemaphore(target).release();
            } catch (Exception ex) {

            }
        }
    }

    private GithubGeracaoVersaoResultDTO getResultWorkflow(Long runId, String repository) throws Exception {
        GithubWorkflowDTO workflow = this.getGithubService().getWorkFlow(repository, runId);

        GithubGeracaoVersaoResultDTO result = new GithubGeracaoVersaoResultDTO();
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
                result = mapper.readValue(json, GithubGeracaoVersaoResultDTO.class);

            }
            result.setConclusionDate(workflow.getUpdated_at());
        }
        result.setStatus(workflow.getStatus());
        return result;
    }

    @Async
    public void initScheduled() {
        List<AtualizacaoVersaoBean> beans = this.getRepository().findAllBySchedulerDateAfterAndStatus(Calendar.getInstance(), GithubActionStatusEnum.scheduled);
        if (beans != null && !beans.isEmpty()) {
            for (AtualizacaoVersaoBean bean: beans) {
                this.schedule(bean.getId(), bean.getSchedulerDate().getTime());
            }
        }
    }

    public void schedule(Long id, Date schedulerDate) {

        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                AtualizacaoVersaoBean bean = null;
                try {
                    Map.Entry<AtualizacaoVersaoBean, Throwable> ret = service.generateVersion(id);
                    bean = ret.getKey();
                    if (ret.getValue() != null) {
                        bean.setStatus(GithubActionStatusEnum.completed);
                        bean.setConclusionDate(Calendar.getInstance());
                        bean.setConclusion(GithubActionConclusionEnum.scheduler_error);
                        bean.setError(ExceptionUtils.getStackTrace(ret.getValue()));
                        service.getRepository().save(bean);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    schedulers.remove(id);
                }
            }
        }, schedulerDate);
        cancelSchedule(id);
        schedulers.put(id, threadPoolTaskScheduler);

    }

    @Transactional
    public AtualizacaoVersaoBean generate(AtualizacaoVersaoBean body) throws Exception {
        if (body.getId() != null && body.getId() > 0) {
            AtualizacaoVersaoBean bean = this.getRepository().findById(body.getId()).get();
            if (bean.getStatus() != GithubActionStatusEnum.scheduled) {
                throw new Exception("Apenas requisições agendadas podem ser modificadas");
            }
        }
        AmbienteBean ambiente = this.getAmbienteRepository().findById(body.getEnvironment().getId()).get();
        body.setEnvironment(ambiente);

        for (AtualizacaoVersaoModuloBean mod: body.getModules()) {
            mod.setTag(body.getTag());
            mod.setBuild("SNAPSHOT");
        }

        if (body.getStatus() != GithubActionStatusEnum.scheduled) {
            this.service.generateVersion(body);
            return this.getRepository().save(body);
        } else {
            body.setWorkflow(null);
            long diff = body.getSchedulerDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes < 2) {
                //throw new Exception("A data/hora de agendamento deve estar, ao menos, 10 minutos a frente da data/hora atual");
            }
            body = this.getRepository().save(body);
            this.schedule(body.getId(), body.getSchedulerDate().getTime());
            return body;
        }
    }


    public GithubVersionService getGithubService() {
        return this.service.getGithubService();
    }

    public AtualizacaoVersaoRepository getRepository() {
        return this.service.getRepository();
    }

    public AmbienteRepository getAmbienteRepository() {
        return this.service.getAmbienteRepository();
    }

    public VersaoRepository getVersaoRepository() {
        return this.service.getVersaoRepository();
    }

    public String getGithubIdentifier(long id, String title) throws Exception {
        return this.service.getGithubIdentifier(id, title);
    }

    @Transactional
    @Async
    public void deleteOldResults() {
        try {
            GithubConfigDTO config = this.service.getGithubService().getConfig();
            if (config.getDaysForKeep() == null || config.getDaysForKeep().equals(0)) {
                return;
            }
            deleteSemaphore.acquire();
            Calendar limitDate = Calendar.getInstance();
            limitDate.add(Calendar.DATE, config.getDaysForKeep() * -1);
            List<AtualizacaoVersaoBean> beans = this.getRepository().findAllByRequestDateBeforeAndStatus(limitDate, GithubActionStatusEnum.completed);
            this.getRepository().deleteAll(beans);

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
