package br.com.evolui.portalevolui.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Remove sessões fechadas de {@link WebSocketConfig#connectedDevices}. Evita entradas órfãs quando a conexão
 * cai sem {@code afterConnectionClosed} limpo (firewall, proxy, etc.), em conjunto com heartbeat STOMP.
 */
@Component
public class WebSocketStaleSessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(WebSocketStaleSessionCleanup.class);

    @Scheduled(fixedDelayString = "${evolui.websocket.stale-session-cleanup-ms:60000}")
    public void purgeStaleEntries() {
        int before = WebSocketConfig.connectedDevices.size();
        WebSocketConfig.purgeClosedSessions();
        int after = WebSocketConfig.connectedDevices.size();
        if (log.isDebugEnabled() && before != after) {
            log.debug("WebSocket connectedDevices cleanup: {} -> {} entries", before, after);
        }
    }
}
