package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.HealthCheckerEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.NotificationBasicConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.NotificationConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.NotificationTriggerConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.NotificationTriggerEnum;
import br.com.evolui.portalevolui.web.rest.dto.enums.NotificationTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.version.*;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService implements ISystemConfigService {
    private NotificationConfigDTO config;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private SMTPService smtpService;

    @Autowired
    private GoogleService googleService;

    @Autowired
    private GithubVersionService githubVersionService;
    @Autowired
    private AWSService awsService;

    @Autowired
    private MondayService mondayService;

    @Transactional
    public void sendVersionSync(GeracaoVersaoBean bean, String workflowLink) throws Exception {
        this.sendVersion(bean, false);
    }

    @Transactional
    public void sendVersionAsync(GeracaoVersaoBean bean) throws Exception {
        this.sendVersion(bean, true);
    }

    @Transactional
    public void sendUpdateVersionSync(AtualizacaoVersaoBean bean) throws Exception {
        this.sendUpdate(bean, false);
    }

    @Transactional
    public void sendUpdateVersionAsync(AtualizacaoVersaoBean bean) throws Exception {
        this.sendUpdate(bean, true);
    }

    @Transactional
    public void sendCICDSync(CICDBean bean) throws Exception {
        this.sendCICD(bean, false);
    }

    @Transactional
    public void sendCICDAsync(CICDBean bean) throws Exception {
        this.sendCICD(bean, true);
    }

    @Transactional
    public void sendHealthCheckerSync(HealthCheckerBean bean, HealthCheckerBean body, String link) throws Exception {
        this.sendHealthChecker(bean, body, link, false);
    }

    @Transactional
    public void sendHealthCheckerAsync(HealthCheckerBean bean, HealthCheckerBean body, String link) throws Exception {
        this.sendHealthChecker(bean, body, link, true);
    }

    @Transactional
    public void sendBackupRestoreSync(ActionRDSBean bean) throws Exception {
        this.sendBackupRestore(bean, false);
    }

    @Transactional
    public void sendBackupRestoreAsync(ActionRDSBean bean) throws Exception {
        this.sendBackupRestore(bean, true);
    }

    private void sendVersion(GeracaoVersaoBean bean, boolean async) throws Exception {

        try {
            if (!this.awsService.initialize() || !this.githubVersionService.initialize()) {
                return;
            }
            List<NotificationTriggerConfigDTO> triggers = this.getConfig().getConfigs().stream().filter(x ->
                            (x.getReferences() == null || x.getReferences().isEmpty() || x.getReferences().contains(bean.getProject().getId().toString()))
                                    && x.getTriggerType() == NotificationTriggerEnum.VERSION_CREATION)
                    .collect(Collectors.toList());

            for (NotificationTriggerConfigDTO trigger : triggers) {
                for (Map.Entry<NotificationTypeEnum, NotificationBasicConfigDTO> c : trigger.getConfigs().entrySet()) {
                    NotificationBasicConfigDTO n = c.getValue();
                    if (n.getEnabled() != null && n.getEnabled().booleanValue()) {
                        if (c.getKey() == NotificationTypeEnum.EMAIL) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getProject().getRepository(), bean.getWorkflow());
                            GeracaoVersaoEmailNotificationDTO dto = GeracaoVersaoEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.smtpService.sendVersionAsync(dto);
                            } else {
                                this.smtpService.sendVersionSync(dto);
                            }
                        } else if (c.getKey() == NotificationTypeEnum.GOOGLE_CHAT) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getProject().getRepository(), bean.getWorkflow());
                            GeracaoVersaoEmailNotificationDTO dto = GeracaoVersaoEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.googleService.sendVersionAsync(dto);
                            } else {
                                this.googleService.sendVersionSync(dto);
                            }
                        }
                    }
                }
            }
            if (bean.getConclusion() != null && bean.getConclusion() == GithubActionConclusionEnum.success && this.mondayService.versionGerenarionIsEnabled()) {
                if (bean.getCompileType() == CompileTypeEnum.beta ||
                        (bean.getCompileType() == CompileTypeEnum.patch && !bean.getModules().stream().anyMatch(x -> x.getProjectModule().isMain() && x.isEnabled()))) {
                    return;
                }
                GeracaoVersaoMondayNotificationDTO dto = GeracaoVersaoMondayNotificationDTO.fromBean(bean);
                if (async) {
                    this.mondayService.sendVersionAsync(dto);
                } else {
                    this.mondayService.sendVersionSync(dto);
                }
            }
        }
        finally {
            this.awsService.dispose();
            this.githubVersionService.dispose();
        }


    }

    private void sendUpdate(AtualizacaoVersaoBean bean, boolean async) throws Exception {
        try {
            if (!this.awsService.initialize() || !this.githubVersionService.initialize()) {
                return;
            }
            List<NotificationTriggerConfigDTO> triggers = this.getConfig().getConfigs().stream().filter(x ->
                            (x.getReferences() == null || x.getReferences().isEmpty() || x.getReferences().contains(bean.getEnvironment().getId().toString()))
                                    && x.getTriggerType() == NotificationTriggerEnum.VERSION_UPDATE)
                    .collect(Collectors.toList());

            for (NotificationTriggerConfigDTO trigger : triggers) {
                for (Map.Entry<NotificationTypeEnum, NotificationBasicConfigDTO> c : trigger.getConfigs().entrySet()) {
                    NotificationBasicConfigDTO n = c.getValue();
                    if (n.getEnabled() != null && n.getEnabled().booleanValue()) {
                        if (c.getKey() == NotificationTypeEnum.EMAIL) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getEnvironment().getProject().getRepository(), bean.getWorkflow());
                            AtualizacaoVersaoEmailNotificationDTO dto = AtualizacaoVersaoEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.smtpService.sendUpdateAsync(dto);
                            } else {
                                this.smtpService.sendUpdateSync(dto);
                            }
                        } else if (c.getKey() == NotificationTypeEnum.GOOGLE_CHAT) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getEnvironment().getProject().getRepository(), bean.getWorkflow());
                            AtualizacaoVersaoEmailNotificationDTO dto = AtualizacaoVersaoEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.googleService.sendUpdateAsync(dto);
                            } else {
                                this.googleService.sendUpdateSync(dto);
                            }
                        }
                    }
                }
            }
        }
        finally {
            this.awsService.dispose();
            this.githubVersionService.dispose();
        }

    }

    private void sendCICD(CICDBean bean, boolean async) throws Exception {
        try {
            if (!this.awsService.initialize() || !this.githubVersionService.initialize()) {
                return;
            }
            List<NotificationTriggerConfigDTO> triggers = this.getConfig().getConfigs().stream().filter(x ->
                            (x.getReferences() == null || x.getReferences().isEmpty() || x.getReferences().contains(bean.getProject().getId().toString()))
                                    && x.getTriggerType() == NotificationTriggerEnum.CI_CD)
                    .collect(Collectors.toList());

            for (NotificationTriggerConfigDTO trigger : triggers) {
                for (Map.Entry<NotificationTypeEnum, NotificationBasicConfigDTO> c : trigger.getConfigs().entrySet()) {
                    NotificationBasicConfigDTO n = c.getValue();
                    if (n.getEnabled() != null && n.getEnabled().booleanValue()) {
                        if (c.getKey() == NotificationTypeEnum.EMAIL) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getProject().getRepository(), bean.getWorkflow());
                            CICDEmailNotificationDTO dto = CICDEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.smtpService.sendCICDAsync(dto);
                            } else {
                                this.smtpService.sendCICDSync(dto);
                            }
                        } else if (c.getKey() == NotificationTypeEnum.GOOGLE_CHAT) {
                            String workflowLink = this.githubVersionService.getLinkWorkflow(bean.getProject().getRepository(), bean.getWorkflow());
                            CICDEmailNotificationDTO dto = CICDEmailNotificationDTO.fromBean(bean, n.getDestinations(), workflowLink, this.awsService.getLinkBucketVersoes());
                            if (async) {
                                this.googleService.sendCICDAsync(dto);
                            } else {
                                this.googleService.sendCICDSync(dto);
                            }
                        }
                    }
                }
            }
        }
        finally {
            this.awsService.dispose();
            this.githubVersionService.dispose();
        }

    }

    private void sendHealthChecker(HealthCheckerBean bean, HealthCheckerBean body, String link, boolean async) throws Exception {

        List<NotificationTriggerConfigDTO> triggers = this.getConfig().getConfigs().stream().filter(x ->
                        (x.getReferences() == null || x.getReferences().isEmpty() || x.getReferences().contains(bean.getId().toString()))
                                && x.getTriggerType() == NotificationTriggerEnum.HEALTH_CHECKER)
                .collect(Collectors.toList());

        for (NotificationTriggerConfigDTO trigger : triggers) {
            for (Map.Entry<NotificationTypeEnum, NotificationBasicConfigDTO> c : trigger.getConfigs().entrySet()) {
                NotificationBasicConfigDTO n = c.getValue();
                if (n.getEnabled() != null && n.getEnabled().booleanValue()) {
                    if (c.getKey() == NotificationTypeEnum.EMAIL) {
                        HealthCheckerEmailNotificationDTO dto = HealthCheckerEmailNotificationDTO.fromBean(bean, body, n.getDestinations(), link);
                        if (dto == null) {
                            return;
                        }
                        if (async) {
                            this.smtpService.sendHealthCheckerAsync(dto);
                        } else {
                            this.smtpService.sendHealthCheckerSync(dto);
                        }
                    }
                    else if (c.getKey() == NotificationTypeEnum.GOOGLE_CHAT) {
                        HealthCheckerEmailNotificationDTO dto = HealthCheckerEmailNotificationDTO.fromBean(bean, body, n.getDestinations(), link);
                        if (dto == null) {
                            return;
                        }
                        if (async) {
                            this.googleService.sendHealthCheckerAsync(dto);
                        } else {
                            this.googleService.sendHealthCheckerSync(dto);
                        }
                    }
                }
            }
        }

    }

    private void sendBackupRestore(ActionRDSBean bean, boolean async) throws Exception {
        try {

            List<NotificationTriggerConfigDTO> triggers = this.getConfig().getConfigs().stream().filter(x ->
                            (x.getReferences() == null || x.getReferences().isEmpty() || x.getReferences().contains(bean.getRds().getEndpoint().toString()))
                                    && x.getTriggerType() == NotificationTriggerEnum.BACKUP_RESTORE)
                    .collect(Collectors.toList());

            for (NotificationTriggerConfigDTO trigger : triggers) {
                for (Map.Entry<NotificationTypeEnum, NotificationBasicConfigDTO> c : trigger.getConfigs().entrySet()) {
                    NotificationBasicConfigDTO n = c.getValue();
                    if (n.getEnabled() != null && n.getEnabled().booleanValue()) {
                        if (c.getKey() == NotificationTypeEnum.EMAIL) {
                            BackupRestoreEmailNotificationDTO dto = BackupRestoreEmailNotificationDTO.fromBean(bean, n.getDestinations());
                            if (async) {
                                this.smtpService.sendBackupRestoreAsync(dto);
                            } else {
                                this.smtpService.sendBackupRestoreAsync(dto);
                            }
                        } else if (c.getKey() == NotificationTypeEnum.GOOGLE_CHAT) {
                            BackupRestoreEmailNotificationDTO dto = BackupRestoreEmailNotificationDTO.fromBean(bean, n.getDestinations());
                            if (async) {
                                this.googleService.sendBackupRestoreAsync(dto);
                            } else {
                                this.googleService.sendBackupRestoreSync(dto);
                            }
                        }
                    }
                }
            }
        }
        finally {
            this.awsService.dispose();
            this.githubVersionService.dispose();
        }

    }
    @Override
    public boolean initialize(Object... param) {
        return this.getConfig() != null;
    }

    public void dispose() {
        this.config = null;
    }

    @Override
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.NOTIFICATION).orElse(null);
    }

    public NotificationConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (NotificationConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(NotificationConfigDTO dto) {
        this.config = dto;
    }
}
