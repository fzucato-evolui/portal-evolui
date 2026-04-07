package br.com.evolui.portalevolui.web.websocket.controller;

import br.com.evolui.portalevolui.shared.dto.RunnerInstallerBlockedDTO;
import br.com.evolui.portalevolui.shared.dto.RunnerInstallerHelloDTO;
import br.com.evolui.portalevolui.shared.dto.WebSocketMessageDTO;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerMessageTopicConstants;
import br.com.evolui.portalevolui.shared.enums.RunnerInstallerMessageTopicConstants;
import br.com.evolui.portalevolui.web.rest.dto.config.GithubConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.runner.*;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import br.com.evolui.portalevolui.web.util.SemverUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Objects;

/**
 * STOMP para o fluxo do instalador de runner. Valida semver apenas em {@link RunnerInstallerMessageTopicConstants#RUNNER_INSTALL_HELLO};
 * demais tópicos são relay; browser e Go usam o mesmo sufixo de fila {@code uuid} da sessão do modal (evita colisão entre abas do mesmo JWT).
 */
@Controller
public class RunnerInstallerWebsocketController {

    private static final Logger log = LoggerFactory.getLogger(RunnerInstallerWebsocketController.class);

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private GithubVersionService githubVersionService;

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_HELLO)
    public void runnerInstallHello(@Payload WebSocketMessageDTO<RunnerInstallerHelloDTO> message, Principal principal) {
        try {
            if (message == null || !StringUtils.hasText(message.getTo()) || !StringUtils.hasText(message.getFrom())) {
                log.warn("runner-install-hello: mensagem sem to/from");
                return;
            }
            RunnerInstallerHelloDTO hi = message.getMessage();
            if (hi == null || !StringUtils.hasText(hi.getClientVersion())) {
                sendVersionBlocked(message, "Versão do client ausente", null,
                        hi != null ? hi.getClientVersion() : null, null);
                return;
            }
            try {
                if (!githubVersionService.initialize()) {
                    sendVersionBlocked(message, "GitHub não configurado no portal", null,
                            hi.getClientVersion(), null);
                    return;
                }
                GithubConfigDTO cfg = githubVersionService.getConfig();
                String min = cfg.getRunnerInstallerMinVersion();
                if (StringUtils.hasText(min) && !SemverUtil.isAtLeast(hi.getClientVersion(), min)) {
                    sendVersionBlocked(message, "Versão do instalador abaixo do mínimo exigido pelo portal", min,
                            hi.getClientVersion(), cfg.getRunnerInstallerDownloadUrl());
                    return;
                }
                simpMessagingTemplate.convertAndSend(
                        String.format("/queue/%s/%s", RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_HELLO, message.getTo()),
                        message);
            } finally {
                githubVersionService.dispose();
            }
        } catch (Exception e) {
            log.error("runner-install-hello: falha", e);
            sendRoutingFailure(message, e.getMessage());
        }
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_REQUEST)
    public void machineInfoRequest(@Payload WebSocketMessageDTO<Void> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_REQUEST, message);
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_RESPONSE)
    public void machineInfoResponse(@Payload WebSocketMessageDTO<RunnerInstallMachineInfoResponseDTO> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_RESPONSE, message);
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_REQUEST)
    public void workdirCheckRequest(@Payload WebSocketMessageDTO<RunnerInstallWorkdirCheckRequestDTO> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_REQUEST, message);
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_RESPONSE)
    public void workdirCheckResponse(@Payload WebSocketMessageDTO<RunnerInstallWorkdirCheckResponseDTO> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_RESPONSE, message);
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_CONFIG)
    public void installConfig(@Payload WebSocketMessageDTO<RunnerInstallConfigDTO> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_CONFIG, message);
    }

    @MessageMapping("/" + RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_RESULT)
    public void installResult(@Payload WebSocketMessageDTO<RunnerInstallResultDTO> message, Principal principal) {
        relay(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_RESULT, message);
    }

    private void relay(String topic, WebSocketMessageDTO<?> message) {
        try {
            if (message == null || !StringUtils.hasText(message.getTo())) {
                log.warn("runner-install topic {}: destino ausente", topic);
                return;
            }
            simpMessagingTemplate.convertAndSend(String.format("/queue/%s/%s", topic, message.getTo()), message);
        } catch (Exception e) {
            log.error("runner-install topic {}: relay falhou", topic, e);
            sendRoutingFailure(message, e.getMessage());
        }
    }

    private void sendVersionBlocked(WebSocketMessageDTO<RunnerInstallerHelloDTO> original, String reason,
                                    String minVersion, String clientVersion, String installerDownloadUrl) {
        if (original == null || !StringUtils.hasText(original.getTo()) || !StringUtils.hasText(original.getFrom())) {
            return;
        }
        RunnerInstallerBlockedDTO blocked = new RunnerInstallerBlockedDTO();
        blocked.setReason(reason);
        blocked.setMinVersionRequired(minVersion);
        blocked.setClientVersion(clientVersion);
        blocked.setInstallerDownloadUrl(installerDownloadUrl);

        WebSocketMessageDTO<RunnerInstallerBlockedDTO> toBrowser = new WebSocketMessageDTO<>();
        toBrowser.setFrom("server");
        toBrowser.setTo(original.getTo());
        toBrowser.setMessage(blocked);
        toBrowser.setError(reason);
        simpMessagingTemplate.convertAndSend(
                String.format("/queue/%s/%s", RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_BLOCKED, original.getTo()),
                toBrowser);

        if (!Objects.equals(original.getTo(), original.getFrom())) {
            WebSocketMessageDTO<RunnerInstallerBlockedDTO> toClient = new WebSocketMessageDTO<>();
            toClient.setFrom("server");
            toClient.setTo(original.getFrom());
            toClient.setMessage(blocked);
            toClient.setError(reason);
            simpMessagingTemplate.convertAndSend(
                    String.format("/queue/%s/%s", RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_BLOCKED, original.getFrom()),
                    toClient);
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
            log.error("runner-install: falha ao enviar routing-failure", ex);
        }
    }
}
