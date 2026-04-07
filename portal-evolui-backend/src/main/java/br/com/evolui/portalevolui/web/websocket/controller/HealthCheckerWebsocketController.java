package br.com.evolui.portalevolui.web.websocket.controller;

import br.com.evolui.portalevolui.shared.dto.*;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerMessageTopicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;

@Controller
public class HealthCheckerWebsocketController {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckerWebsocketController.class);

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    private <T> void route(String topic, WebSocketMessageDTO<T> message, Runnable forward) {
        try {
            if (message == null || !StringUtils.hasText(message.getTo())) {
                log.warn("WebSocket topic {}: missing message or destination", topic);
                return;
            }
            forward.run();
        } catch (Exception e) {
            log.error("WebSocket topic {}: routing failed", topic, e);
            sendRoutingFailure(message, e.getMessage());
        }
    }

    private void sendRoutingFailure(WebSocketMessageDTO<?> message, String rawError) {
        if (message == null || !StringUtils.hasText(message.getTo())) {
            return;
        }
        try {
            WebSocketMessageDTO<String> err = new WebSocketMessageDTO<>();
            err.setFrom("server");
            err.setTo(message.getTo());
            err.setError(rawError != null ? rawError : "Erro no servidor");
            simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                    HealthCheckerMessageTopicConstants.ROUTING_FAILURE, message.getTo()), err);
        } catch (Exception ex) {
            log.error("WebSocket: failed to notify routing failure to {}", message.getTo(), ex);
        }
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.HEY)
    public void executeHey(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.HEY, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.HEY,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.START_REQUEST)
    public void executeStartRequest(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.START_REQUEST, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.START_REQUEST,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.START_RESPONSE)
    public void executeStartResponse(@Payload WebSocketMessageDTO<HealthCheckerSystemInfoDTO> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.START_RESPONSE, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.START_RESPONSE,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST)
    public void executeCommandRequest(@Payload WebSocketMessageDTO<String> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE)
    public void executeCommandResponse(@Payload WebSocketMessageDTO<ConsoleResponseMessageDTO> message) {
        route(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START)
    public void executeSystemInfoStart(@Payload WebSocketMessageDTO<Boolean> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP)
    public void executeSystemInfoStop(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE)
    public void executeSystemInfoReponse(@Payload WebSocketMessageDTO<HealthCheckerSystemInfoDTO> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST)
    public void testConfigRequest(@Payload WebSocketMessageDTO<HealthCheckerModuleConfigDTO> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE)
    public void testConfigResponse(@Payload WebSocketMessageDTO<HealthCheckerModuleDTO> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST)
    public void executeSaveConfigRequest(@Payload WebSocketMessageDTO<HealthCheckerConfigDTO> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST,
                        message.getTo()), message));
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE)
    public void executeSaveConfigResponse(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        route(HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE, message, () ->
                simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                        HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE,
                        message.getTo()), message));
    }
}
