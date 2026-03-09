package br.com.evolui.portalevolui.web.websocket.controller;

import br.com.evolui.portalevolui.shared.dto.*;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerMessageTopicConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class HealthCheckerWebsocketController {
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @MessageMapping("/" + HealthCheckerMessageTopicConstants.HEY)
    public void executeHey(@Payload WebSocketMessageDTO<Void> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.HEY,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.START_REQUEST)
    public void executeStartRequest(@Payload WebSocketMessageDTO<Void> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.START_REQUEST,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.START_RESPONSE)
    public void executeStartResponse(@Payload WebSocketMessageDTO<HealthCheckerSystemInfoDTO> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.START_RESPONSE,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST)
    public void executeCommandRequest(@Payload WebSocketMessageDTO<String> message, Principal principal) {
        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST,
                message.getTo()), message);
    }
    @MessageMapping("/" + HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE)
    public void executeCommandResponse(@Payload WebSocketMessageDTO<ConsoleResponseMessageDTO> message) {
        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START)
    public void executeSystemInfoStart(@Payload WebSocketMessageDTO<Boolean> message, Principal principal) {
        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP)
    public void executeSystemInfoStop(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE)
    public void executeSystemInfoReponse(@Payload WebSocketMessageDTO<HealthCheckerSystemInfoDTO> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST)
    public void testConfigRequest(@Payload WebSocketMessageDTO<HealthCheckerModuleConfigDTO> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE)
    public void testConfigResponse(@Payload WebSocketMessageDTO<HealthCheckerModuleDTO> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST)
    public void executeSaveConfigRequest(@Payload WebSocketMessageDTO<HealthCheckerConfigDTO> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST,
                message.getTo()), message);
    }

    @MessageMapping("/" + HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE)
    public void executeSaveConfigResponse(@Payload WebSocketMessageDTO<Void> message, Principal principal) {

        this.simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s",
                HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE,
                message.getTo()), message);
    }


}
