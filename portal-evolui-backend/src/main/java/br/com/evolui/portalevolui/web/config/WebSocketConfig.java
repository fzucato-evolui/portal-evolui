package br.com.evolui.portalevolui.web.config;

import br.com.evolui.portalevolui.shared.enums.HealthCheckerMessageTopicConstants;
import br.com.evolui.portalevolui.web.component.JwtUtilComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ThreadPoolTaskScheduler webSocketHeartbeatScheduler;

    @Autowired
    private JwtUtilComponent jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketConfig(@Qualifier("webSocketHeartbeatScheduler") ThreadPoolTaskScheduler webSocketHeartbeatScheduler) {
        this.webSocketHeartbeatScheduler = webSocketHeartbeatScheduler;
    }

    public static ConcurrentHashMap<String, WebSocketSession> connectedDevices = new ConcurrentHashMap<>();

    private static final int WS_TEXT_BINARY_BUFFER_BYTES = 512 * 1024;

    /**
     * Remove entradas órfãs (sessão fechada ou nula). Complementa o heartbeat STOMP para evitar mapa crescente.
     */
    public static void purgeClosedSessions() {
        Iterator<Map.Entry<String, WebSocketSession>> it = connectedDevices.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WebSocketSession> e = it.next();
            WebSocketSession s = e.getValue();
            if (s == null || !s.isOpen()) {
                it.remove();
            }
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        RequestUpgradeStrategy upgradeStrategy = new TomcatRequestUpgradeStrategy();
        registry.addEndpoint("/portalEvoluiWebSocket")
                .setHandshakeHandler(new DefaultHandshakeHandler(upgradeStrategy))
                .setAllowedOrigins("*")
                .withSockJS();
        registry.addEndpoint("/portalEvoluiWebSocket")
                .setHandshakeHandler(new DefaultHandshakeHandler(upgradeStrategy))
                .setAllowedOrigins("*");
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic/", "/queue/")
                .setHeartbeatValue(new long[]{10_000, 10_000})
                .setTaskScheduler(webSocketHeartbeatScheduler);
        config.setApplicationDestinationPrefixes("/app");
    }

    /*
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                StompCommand cmd = accessor.getCommand();
                if (StompCommand.SEND == cmd) {
                    UsernamePasswordAuthenticationToken authentication;
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authorization)) {
                        String jwt = authorization.replace("Bearer ", "");
                        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                            String username = jwtUtils.getUserNameFromJwtToken(jwt);

                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            SecurityContextHolder.getContext().setAuthentication(authentication);

                        }

                        accessor.setUser(SecurityContextHolder.getContext().getAuthentication());

                    }
                }

                return message;
            }
        });
    }

     */

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(WS_TEXT_BINARY_BUFFER_BYTES);
        container.setMaxBinaryMessageBufferSize(WS_TEXT_BINARY_BUFFER_BYTES);

        return container;
    }
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(WS_TEXT_BINARY_BUFFER_BYTES);
        registration.setSendTimeLimit(120_000);
        registration.setSendBufferSizeLimit(WS_TEXT_BINARY_BUFFER_BYTES);
        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler handler) {
                return new WebSocketHandlerDecorator(handler) {

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        super.afterConnectionEstablished(session);
                        String identifier = getIdentifier(session);
                        if (StringUtils.hasText(identifier)) {
                            connectedDevices.put(identifier, session);
                        }
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                        super.afterConnectionClosed(session, closeStatus);

                        String identifier = getIdentifier(session);
                        if (StringUtils.hasText(identifier)) {
                            connectedDevices.remove(identifier);
                            LinkedHashMap<String, String> message = new LinkedHashMap<>();
                            message.put("client", identifier);

                            simpMessagingTemplate.convertAndSend(String.format("/topic/%s",
                                    HealthCheckerMessageTopicConstants.CLIENT_DISCONECTION), message);
                        }
                    }
                };
            }
        });
    }

    private String getIdentifier(WebSocketSession session) {
        List<String> header = session.getHandshakeHeaders().get("Identifier");
        if (header != null && !header.isEmpty()) {
            return header.get(0);

        } else {
            MultiValueMap<String, String> parameters =
                    UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();
            if (parameters.containsKey("Authorization")) {
               return parameters.get("Authorization").get(0);
            }
        }
        return null;
    }

}
