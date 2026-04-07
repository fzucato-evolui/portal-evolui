package br.com.evolui.portalevolui.shared.enums;

/**
 * Tópicos STOMP do instalador de self-hosted runner. Prefixo de aplicação: {@code /app}; broker: {@code /queue}, {@code /topic}.
 * <p>
 * Roteamento (igual ao health-checker): mensagem {@code WebSocketMessageDTO} com {@code from} e {@code to}.
 * Browser e client Go assinam {@code /queue/&lt;topic&gt;/&lt;uuidSessão&gt;} (o mesmo UUID do modal), para isolar abas e usuários com o mesmo JWT.
 * O JWT do utilizador serve só à autenticação WebSocket do Go (query {@code Authorization}), não ao sufixo das filas.
 * <p>
 * {@code to} em cada envio é o {@code uuid} da sessão do destinatário.
 */
public final class RunnerInstallerMessageTopicConstants {

    private RunnerInstallerMessageTopicConstants() {
    }

    /**
     * Servidor → browser e client: client abaixo da versão mínima. Corpo: {@code WebSocketMessageDTO} com {@code RunnerInstallerBlockedDTO} em {@code message}.
     */
    public static final String RUNNER_INSTALL_BLOCKED = "runner-install-blocked";

    /**
     * Cliente Go → servidor → browser: apresentação (versão, SO). Servidor valida semver antes de encaminhar.
     */
    public static final String RUNNER_INSTALL_HELLO = "runner-install-hello";

    /**
     * Browser → client: pedir dados da máquina / requisitos.
     */
    public static final String RUNNER_INSTALL_MACHINE_INFO_REQUEST = "runner-install-machine-info-request";

    /**
     * Client → browser: resposta com informações da máquina.
     */
    public static final String RUNNER_INSTALL_MACHINE_INFO_RESPONSE = "runner-install-machine-info-response";

    /**
     * Browser → client: validar se pasta de trabalho existe (e opcionalmente gravável).
     */
    public static final String RUNNER_INSTALL_WORKDIR_CHECK_REQUEST = "runner-install-workdir-check-request";

    /**
     * Client → browser: resultado da verificação da pasta.
     */
    public static final String RUNNER_INSTALL_WORKDIR_CHECK_RESPONSE = "runner-install-workdir-check-response";

    /**
     * Browser → client: payload final (inclui registration token obtido via REST no “Instalar agora”).
     */
    public static final String RUNNER_INSTALL_CONFIG = "runner-install-config";

    /**
     * Client → browser: sucesso ou falha após tentativa de instalação.
     */
    public static final String RUNNER_INSTALL_RESULT = "runner-install-result";
}
