package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.action_rds.ActionRDSRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.IActionRDSHelperService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public abstract class ActionRDSHelperService implements IActionRDSHelperService {
    private static final ConcurrentHashMap<Long, BackupRestoreRDSDTO> backupRestoreMap = new ConcurrentHashMap<>();
    @Autowired
    private AWSService service;

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    private ActionRDSRepository repository;

    public AWSService getService() {
        return service;
    }

    @Override
    @Transactional(propagation=REQUIRES_NEW)
    public void backup(ActionRDSBean bean) {
        BackupRestoreRDSDTO dto = new BackupRestoreRDSDTO(bean);
        Future<BackupRestoreRDSDTO> process = this.engineBackup(dto);
        backupRestoreMap.put(bean.getId(), dto);
        BackupRestoreRDSDTO result;
        try {
            result = process.get();
        } catch (Exception ex) {
            result = new BackupRestoreRDSDTO(bean);
            result.setError(ex);
        }
        ActionRDSBean currentBean = getRepository().findById(bean.getId()).get();
        currentBean.setStatus(GithubActionStatusEnum.completed);
        currentBean.setConclusionDate(Calendar.getInstance());
        currentBean.setRestoreKey(result.getBean().getRestoreKey());
        if (result.getError()!= null) {
            if (!result.isCanceled()) {
                currentBean.setConclusion(GithubActionConclusionEnum.failure);
            } else {
                currentBean.setConclusion(GithubActionConclusionEnum.cancelled);
            }
            currentBean.setError(ExceptionUtils.getStackTrace(result.getError()));
        } else {
            currentBean.setConclusion(GithubActionConclusionEnum.success);
        }
        getRepository().save(currentBean);
        BackupRestoreRDSStatusDTO status = new BackupRestoreRDSStatusDTO("Backup finalizado", Level.INFO, this.getClass(), true);
        status.setFinished(true);
        result.addStatus(status);
        backupRestoreMap.remove(bean.getId());
        try {
            notificationService.sendBackupRestoreAsync(currentBean);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(propagation=REQUIRES_NEW)
    public void restore(ActionRDSBean bean) {
        BackupRestoreRDSDTO dto = new BackupRestoreRDSDTO(bean);
        Future<BackupRestoreRDSDTO> process = this.engineRestore(dto);
        backupRestoreMap.put(bean.getId(), dto);
        BackupRestoreRDSDTO result;
        try {
            result = process.get();
        } catch (Exception ex) {
            result = new BackupRestoreRDSDTO(bean);
            result.setError(ex);
        }
        ActionRDSBean currentBean = getRepository().findById(bean.getId()).get();
        currentBean.setStatus(GithubActionStatusEnum.completed);
        currentBean.setConclusionDate(Calendar.getInstance());
        currentBean.setRestoreKey(result.getBean().getRestoreKey());
        if (result.getError() != null) {
            if (!result.isCanceled()) {
                currentBean.setConclusion(GithubActionConclusionEnum.failure);
            } else {
                currentBean.setConclusion(GithubActionConclusionEnum.cancelled);
            }
            currentBean.setError(ExceptionUtils.getStackTrace(result.getError()));
        }
        else if (StringUtils.hasText(result.getWarning())) {
            if (!result.isCanceled()) {
                currentBean.setConclusion(GithubActionConclusionEnum.warning);
            }
            else {
                currentBean.setConclusion(GithubActionConclusionEnum.cancelled);
            }
            currentBean.setError(result.getWarning());
        }
        else {
            currentBean.setConclusion(GithubActionConclusionEnum.success);
        }
        getRepository().save(currentBean);
        BackupRestoreRDSStatusDTO status = new BackupRestoreRDSStatusDTO("Restore finalizado", Level.INFO, this.getClass(), true);
        status.setFinished(true);
        result.addStatus(status);
        backupRestoreMap.remove(bean.getId());
        try {
            notificationService.sendBackupRestoreAsync(currentBean);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract Future<BackupRestoreRDSDTO> engineBackup(BackupRestoreRDSDTO dto);
    protected abstract Future<BackupRestoreRDSDTO> engineRestore(BackupRestoreRDSDTO dto);

    public ActionRDSRepository getRepository() {
        return repository;
    }

    public BackupRestoreRDSDTO getBackupRestore(Long id) {
        return backupRestoreMap.get(id);
    }


}
