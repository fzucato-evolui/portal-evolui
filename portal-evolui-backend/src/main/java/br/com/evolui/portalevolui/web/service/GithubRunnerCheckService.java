package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.GithubConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerDTO;
import br.com.evolui.portalevolui.web.util.FunctionsUtil;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GithubRunnerCheckService {
    private GithubConfigDTO config;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private GithubVersionService githubVersionService;

    @Autowired
    private NotificationService notificationService;

    private static ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

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
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.GITHUB).orElse(null);
    }

    public GithubConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (GithubConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(GithubConfigDTO dto) {
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
        if (this.config == null
                || this.config.getRunnerCheckEnabled() == null || !this.config.getRunnerCheckEnabled().booleanValue()
                || StringHelper.isEmpty(this.config.getRunnerCheckCronExpression())) {
            return;
        }

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.initialize();
        CronTrigger cronTrigger = new CronTrigger(FunctionsUtil.normalizeCronExpression(this.config.getRunnerCheckCronExpression()));
        Runnable r = new Runnable() {
            @Override
            public void run() {
                checkRunners();
            }
        };
        scheduler.schedule(r, cronTrigger);
    }

    public void checkRunners() {
        try {
            if (!this.githubVersionService.initialize()) {
                return;
            }
            List<GithubRunnerDTO> offlineRunners;
            try {
                List<GithubRunnerDTO> runners = this.githubVersionService.getRunners().getRunners();
                if (runners == null || runners.isEmpty()) {
                    return;
                }
                offlineRunners = runners.stream()
                        .filter(r -> r.getStatus() == null || !r.getStatus().equalsIgnoreCase("online"))
                        .collect(Collectors.toList());
            } finally {
                this.githubVersionService.dispose();
            }
            if (offlineRunners.isEmpty()) {
                return;
            }
            try {
                if (this.notificationService.initialize()) {
                    try {
                        this.notificationService.sendRunnersOfflineAsync(offlineRunners);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            } finally {
                this.notificationService.dispose();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
