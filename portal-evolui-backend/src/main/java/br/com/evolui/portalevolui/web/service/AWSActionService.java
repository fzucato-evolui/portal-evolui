package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.enums.AWSActionTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.ActionRDSTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.action_rds.ActionRDSRepository;
import br.com.evolui.portalevolui.web.repository.log_aws.LogAWSActionRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BucketDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSAccountConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSConfigDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.IActionRDSHelperService;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class AWSActionService {

    @Autowired
    private ActionRDSOracleHelperService oracleHelperService;

    @Autowired
    private ActionRDSSqlServerHelperService sqlServerHelperService;

    @Autowired
    private ActionRDSPostgresHelperService postgresHelperService;

    @Autowired
    private LogAWSActionRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    private static Semaphore deleteSemaphore = new Semaphore(1);

    private static LinkedHashMap<ActionRDSBean, ThreadPoolTaskScheduler> schedulers = new LinkedHashMap<>();

    public static Semaphore getDeleteSemaphore() {
        return deleteSemaphore;
    }

    public LinkedHashSet<String> retrieveRDSSchemas(RDSDTO rds) throws SQLException {
        ActionRDSBean bean = new ActionRDSBean();
        bean.setRds(rds);
        IActionRDSHelperService helper = getHelperService(bean);
        if (helper != null) {
            return helper.retrieveRDSSchemas(rds);
        }
        return null;
    }

    public LinkedHashSet<String> retrieveRDSTablespaces(RDSDTO rds) throws SQLException {
        ActionRDSBean bean = new ActionRDSBean();
        bean.setRds(rds);
        IActionRDSHelperService helper = getHelperService(bean);
        if (helper != null) {
            return helper.retrieveRDSTablespaces(rds);
        }
        return null;
    }

    public ActionRDSBean backupRestoreRDS(ActionRDSBean bean) throws Exception {
        RDSDTO rds = bean.getRds();
        if (this.isRDSBusy(rds)) {
            throw new Exception("RDS is busy");
        }
        bean.setRequestDate(Calendar.getInstance());
        if (bean.getSchedulerDate() != null) {
            bean.setStatus(GithubActionStatusEnum.scheduled);
        } else {
            bean.setStatus(GithubActionStatusEnum.in_progress);
        }
        bean.setConclusion(null);
        bean.setConclusionDate(null);
        bean.setUser(getLoggedUser());
        bean = this.getRepository().save(bean);
        this.schedule(bean);
        return bean;
    }

    public ActionRDSRepository getRepository() {
        return oracleHelperService.getRepository();
    }

    public LinkedHashMap<String, List<RDSDTO>> getDatabases() {
        LinkedHashMap<String, List<RDSDTO>> resp = new LinkedHashMap<>();
        AWSService service = getAWSService();
        try {
            AWSConfigDTO c = service.getAllConfigs();
            if (c != null && c.getAccountConfigs() != null) {
                for (Map.Entry<String, AWSAccountConfigDTO> e : c.getAccountConfigs().entrySet()) {
                    if (service.initialize(e.getKey())) {
                        resp.put(e.getKey(), service.listRds());
                    }
                }
            }
            return resp;
        }
        finally {
            service.dispose();
        }
    }


    public List<BucketDTO> retrieveBuckets(BucketDTO body) {
        AWSService service = getAWSService();
        try {
            if (service.initialize(body.getAccount())) {
                return service.retrieveBucketFiles(body);
            }
            return null;
        }
        finally {
            service.dispose();
        }
    }

    public LogAWSActionBean createLog(AWSActionTypeEnum actionType, Object dto) throws Exception {
        LogAWSActionBean bean = new LogAWSActionBean();
        bean.setLogDate(Calendar.getInstance());
        bean.setUser(getLoggedUser());
        bean.setActionType(actionType);
        bean.setInstance(new ObjectMapper().writeValueAsString(dto));
        return this.logRepository.save(bean);
    }

    @Transactional
    @Async
    public void deleteOldResults() {
        AWSService service = getAWSService();
        try {
            deleteSemaphore.acquire();
            AWSConfigDTO config = service.getAllConfigs();
            if (config.getDaysForKeep() == null || config.getDaysForKeep().equals(0)) {
                return;
            }
            Calendar limitDate = Calendar.getInstance();
            limitDate.add(Calendar.DATE, config.getDaysForKeep() * -1);
            {
                List<LogAWSActionBean> beans = this.logRepository.findAllByLogDateBefore(limitDate);
                this.logRepository.deleteAll(beans);
            }
            {
                List<ActionRDSBean> beans = this.getRepository().findAllByRequestDateBeforeAndStatus(limitDate, GithubActionStatusEnum.completed);
                this.getRepository().deleteAll(beans);
            }
            {
                List<ActionRDSBean> beans = this.getRepository().findAllByStatusIn(Arrays.asList(GithubActionStatusEnum.in_progress, GithubActionStatusEnum.queued));
                for (ActionRDSBean bean : beans) {
                    if (!processIsRunning(bean)) {
                        bean.setStatus(GithubActionStatusEnum.completed);
                        bean.setConclusion(GithubActionConclusionEnum.cancelled);
                        bean.setConclusionDate(Calendar.getInstance());
                        bean.setError("Processo cancelado de maneira inesperada");
                        this.getRepository().save(bean);
                    }
                }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                deleteSemaphore.release();
            } catch (Exception ex) {

            }
            service.dispose();
        }
    }

    public AWSService getAWSService() {
        return this.oracleHelperService.getService();
    }

    private IActionRDSHelperService getHelperService(ActionRDSBean bean) {
        IActionRDSHelperService helper = null;
        if (bean.getRds().getEngine().toLowerCase().indexOf("aurora") >= 0) {
            return null;
        }
        if (bean.getRds().getEngine().toLowerCase().indexOf("oracle") >= 0) {
            helper = oracleHelperService;
        }
        if (bean.getRds().getEngine().toLowerCase().indexOf("sqlserver") >= 0) {
            helper = sqlServerHelperService;
        }
        if (bean.getRds().getEngine().toLowerCase().indexOf("postgres") >= 0) {
            helper = postgresHelperService;
        }

        return helper;
    }

    public LogAWSActionRepository getLogRepository() {
        return logRepository;
    }

    public boolean isRDSBusy (RDSDTO rds) {
        boolean busy = schedulers.entrySet().stream().anyMatch(e -> e.getKey().getRds().getEndpoint().equals(rds.getEndpoint()));
        return busy;
    }

    public boolean processIsRunning (ActionRDSBean bean) {
        boolean running = schedulers.entrySet().stream().anyMatch(e -> e.getKey().getId().equals(bean.getId()));
        return running;
    }

    @Async
    public void initScheduled() throws Exception {
        List<ActionRDSBean> beans = this.getRepository().findAllBySchedulerDateAfterAndStatus(Calendar.getInstance(), GithubActionStatusEnum.scheduled);
        if (beans != null && !beans.isEmpty()) {
            for (ActionRDSBean bean: beans) {
                this.schedule(bean);
            }
        }
    }

    private void schedule(final ActionRDSBean bean) throws Exception {

        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.initialize();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    IActionRDSHelperService helper = getHelperService(bean);
                    if (helper != null) {
                        if (bean.getActionType().equals(ActionRDSTypeEnum.BACKUP)) {
                            helper.backup(bean);
                        } else if (bean.getActionType().equals(ActionRDSTypeEnum.RESTORE)) {
                            helper.restore(bean);
                        } else if (bean.getActionType().equals(ActionRDSTypeEnum.CLONE)) {
                            helper.clone(bean);
                        }
                    }
                } finally {
                    Map.Entry<ActionRDSBean, ThreadPoolTaskScheduler> scheduler = getScheduler(bean);
                    if (scheduler != null) {
                        scheduler.getValue().destroy();
                        schedulers.remove(scheduler.getKey());
                    }
                }
            }
        };
        if (bean.getStatus() != GithubActionStatusEnum.scheduled) {
            threadPoolTaskScheduler.execute(run);
            schedulers.put(bean, threadPoolTaskScheduler);
        } else {
            long diff = bean.getSchedulerDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

            if (diff < 0) {
                throw new Exception("A data/hora de agendamento ser maior que data/hora atual");
            }
            threadPoolTaskScheduler.schedule(run, bean.getSchedulerDate().getTime());
            cancelSchedule(bean);
            schedulers.put(bean, threadPoolTaskScheduler);
        }


    }

    public static void cancelSchedule(ActionRDSBean bean) {
        Map.Entry<ActionRDSBean, ThreadPoolTaskScheduler> scheduler = getScheduler(bean);
        if (scheduler != null) {
            scheduler.getValue().destroy();
            schedulers.remove(scheduler.getKey());
        }
    }

    public static Map.Entry<ActionRDSBean, ThreadPoolTaskScheduler> getScheduler(ActionRDSBean bean) {
        return schedulers.entrySet().stream().filter(e -> e.getKey().getId().equals(bean.getId())).findFirst().orElse(null);
    }

    private UserBean getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsSecurity user = (UserDetailsSecurity) auth.getPrincipal();
        UserBean userBean = this.userRepository.findById(user.getId()).get();
        return userBean;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void cancel(Long id) {
        BackupRestoreRDSDTO dto = this.getBackupRestore(id);
        if (dto != null) {
            this.getRepository().findById(id).ifPresent(bean -> {
                if (bean.getStatus().equals(GithubActionStatusEnum.in_progress) || bean.getStatus().equals(GithubActionStatusEnum.queued)) {
                    bean.setStatus(GithubActionStatusEnum.canceling);
                    bean.setUser(getLoggedUser());
                    this.getRepository().saveAndFlush(bean);
                    cancelSchedule(bean);

                }
                dto.cancel();
            });
        }

    }

    public BackupRestoreRDSDTO getBackupRestore(Long id) {
        return this.oracleHelperService.getBackupRestore(id);
    }
}
