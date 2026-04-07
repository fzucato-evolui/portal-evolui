package br.com.evolui.portalevolui.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Bean isolado para o heartbeat STOMP do broker simples. Precisa existir antes de
 * {@link WebSocketConfig#configureMessageBroker}; em uma classe separada evita null no injetado por campo.
 */
@Configuration
public class WebSocketHeartbeatSchedulerConfig {

    @Bean(name = "webSocketHeartbeatScheduler")
    public ThreadPoolTaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
