package br.com.evolui.portalevolui.shared.enums;

public class HealthCheckerMessageTopicConstants {
    /**
     * Erro ao rotear mensagem STOMP no servidor. Destino de assinatura do browser/client:
     * {@code /queue/routing-failure/{uuid}} — corpo {@code WebSocketMessageDTO} com {@code error} preenchido.
     */
    public static final String ROUTING_FAILURE = "routing-failure";

    public static final String HEY = "hey";
    public static final String START_REQUEST = "start-request";
    public static final String START_RESPONSE = "start-response";
    public static final String SAVE_CONFIG_REQUEST = "save-config-request";
    public static final String SAVE_CONFIG_RESPONSE = "save-config-response";
    public static final String TEST_CONFIG_REQUEST = "test-config-request";
    public static final String TEST_CONFIG_RESPONSE = "test-config-response";
    public static final String EXECUTE_COMMAND_REQUEST = "execute-command-request";
    public static final String EXECUTE_COMMAND_RESPONSE = "execute-command-response";
    public static final String CLIENT_DISCONECTION = "client-disconnection";
    public static final String SYSTEM_INFO_RESPONSE = "system-info-response";
    public static final String SYSTEM_INFO_REQUEST_START = "system-info-request-start";
    public static final String SYSTEM_INFO_REQUEST_STOP = "system-info-request-stop";
    public static final String SYSTEM_INFO_RESPONSE_STOP = "system-info-response-stop";

}
