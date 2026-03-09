package br.com.evolui.portalevolui.web.config;

import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

//@Configuration
public class WebSocketSecurityConfiguration  extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpDestMatchers("/app/**").permitAll();
    }
}
