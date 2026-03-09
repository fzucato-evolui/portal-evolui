package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import org.slf4j.event.Level;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin/test")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
public class TestAdminRestController {

    @GetMapping(value = "/status/simulate/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter simulateBackupRestoreStatus(@PathVariable("id") Long id) {
        // Criar um ID fictício para a simulação
        final Long simulatedId = System.currentTimeMillis();

        // Configurar um emitter de longa duração
        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                // Simular recuperação de um bean para o log
                boolean isBackup = Math.random() > 0.5; // Aleatoriamente decide se é backup ou restore
                String operationType = isBackup ? "backup" : "restore";
                String dbName = "db_" + (Math.random() > 0.5 ? "prod" : "test") + "_" + simulatedId % 1000;

                // Enviar mensagem inicial
                sendLogMessage(emitter,
                        "Iniciando operação de " + operationType + " para o banco " + dbName,
                        Level.INFO, false);

                // Parâmetros de simulação
                int totalMessages = 200 + new Random().nextInt(800); // Entre 200 e 1000 mensagens
                int currentMessage = 0;
                int burstProbability = 10; // % de chance de burst a cada iteração
                int maxBurstSize = 50;      // Máximo de mensagens em um burst

                // Mensagens detalhadas para parecer realista
                List<String> backupMessages = createBackupMessages(dbName);
                List<String> restoreMessages = createRestoreMessages(dbName);
                List<String> messages = isBackup ? backupMessages : restoreMessages;

                int heartbeatCounter = 0;
                while (currentMessage < totalMessages) {
                    // Verificar se devemos enviar um burst de mensagens
                    boolean isBurst = new Random().nextInt(100) < burstProbability;

                    if (isBurst) {
                        // Determinar o tamanho do burst (entre 5 e maxBurstSize)
                        int burstSize = 5 + new Random().nextInt(maxBurstSize - 5);
                        burstSize = Math.min(burstSize, totalMessages - currentMessage);

                        // Enviar um aviso sobre o início de uma operação intensiva
                        sendLogMessage(emitter,
                                "Iniciando " + (isBackup ? "dump" : "carregamento") + " de dados. Pode haver muitas mensagens.",
                                Level.INFO, false);

                        // Enviar o burst de mensagens (com pequenos intervalos para ser realista)
                        for (int i = 0; i < burstSize; i++) {
                            // Pegar mensagem apropriada ou gerar uma genérica se acabaram as mensagens pré-definidas
                            String msgText;
                            if (currentMessage < messages.size()) {
                                msgText = messages.get(currentMessage);
                            } else {
                                int percentComplete = (currentMessage * 100) / totalMessages;
                                msgText = operationType + " em andamento: " + percentComplete + "% concluído";
                            }

                            // Determinar nível do log (principalmente DEBUG, ocasionalmente INFO ou WARN)
                            Level level = Level.DEBUG;
                            int levelRandom = new Random().nextInt(100);
                            if (levelRandom < 5) {
                                level = Level.WARN;
                            } else if (levelRandom < 15) {
                                level = Level.INFO;
                            }

                            sendLogMessage(emitter, msgText, level, false);
                            currentMessage++;

                            // Pequena pausa entre mensagens do burst (1-10ms)
                            Thread.sleep(1 + new Random().nextInt(10));
                        }

                        // Pausa maior após o burst (500-2000ms)
                        Thread.sleep(500 + new Random().nextInt(1500));
                    } else {
                        // Mensagem normal (não burst)
                        String msgText;
                        if (currentMessage < messages.size()) {
                            msgText = messages.get(currentMessage);
                        } else {
                            int percentComplete = (currentMessage * 100) / totalMessages;
                            msgText = operationType + " em andamento: " + percentComplete + "% concluído";
                        }

                        // Determinar nível do log
                        Level level = Level.DEBUG;
                        int levelRandom = new Random().nextInt(100);
                        if (levelRandom < 2) {
                            level = Level.ERROR; // Raro
                        } else if (levelRandom < 8) {
                            level = Level.WARN;
                        } else if (levelRandom < 20) {
                            level = Level.INFO;
                        }

                        sendLogMessage(emitter, msgText, level, false);
                        currentMessage++;

                        // Pausa normal entre mensagens (100-1000ms)
                        Thread.sleep(100 + new Random().nextInt(900));
                    }

                    // Enviar heartbeat ocasionalmente se não houver atividade por um tempo
                    heartbeatCounter++;
                    if (heartbeatCounter >= 10) {
                        sendHeartbeat(emitter);
                        heartbeatCounter = 0;
                    }
                }

                // Mensagens finais
                sendLogMessage(emitter, "Finalizando operação de " + operationType, Level.INFO, false);

                // 10% de chance de erro no final (para testar essa condição)
                if (new Random().nextInt(100) < 10) {
                    sendLogMessage(emitter, "Erro ao finalizar operação: falha na conexão com o servidor", Level.ERROR, false);
                    sendLogMessage(emitter, operationType + " concluído com erros", Level.ERROR, true);
                } else {
                    sendLogMessage(emitter, operationType + " concluído com sucesso", Level.INFO, true);
                }

                // Finalizar o stream
                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // Configurar callbacks do emitter
        emitter.onCompletion(() -> shutdownExecutor(executor));
        emitter.onTimeout(() -> shutdownExecutor(executor));
        emitter.onError((e) -> {
            shutdownExecutor(executor);
            emitter.complete();
        });

        return emitter;
    }

    // Enviar uma mensagem de heartbeat
    private void sendHeartbeat(SseEmitter emitter) {
        try {
            BackupRestoreRDSStatusDTO heartbeatStatus = new BackupRestoreRDSStatusDTO(
                    "heartbeat",
                    Level.DEBUG,
                    this.getClass(),
                    false
            );
            heartbeatStatus.setHeartbeat(true);
            emitter.send(heartbeatStatus, MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            // Ignorar erros de heartbeat
        }
    }

    // Enviar uma mensagem de log formatada
    private void sendLogMessage(SseEmitter emitter, String message, Level level, boolean finished) throws IOException {
        BackupRestoreRDSStatusDTO status = new BackupRestoreRDSStatusDTO(
                message,
                level,
                this.getClass(),
                finished
        );
        emitter.send(status, MediaType.APPLICATION_JSON);
    }

    // Criar uma lista de mensagens realistas para backup
    private List<String> createBackupMessages(String dbName) {
        List<String> messages = new ArrayList<>();

        // Configuração
        messages.add("Preparando ambiente para backup de " + dbName);
        messages.add("Verificando disponibilidade de espaço em disco");
        messages.add("Espaço em disco disponível: " + (2 + new Random().nextInt(8)) + " GB");
        messages.add("Verificando conexão com o banco de dados " + dbName);
        messages.add("Conexão estabelecida com o banco " + dbName);

        // Processo de backup
        messages.add("Iniciando transação para backup consistente");
        messages.add("Coletando metadados do banco de dados");
        messages.add("Exportando definições de tabelas");

        // Adicionar mensagens por tabela
        String[] tables = {"users", "accounts", "transactions", "logs", "configurations", "reports", "events"};
        for (String table : tables) {
            messages.add("Backup da tabela " + table + " iniciado");
            messages.add("Estimando tamanho da tabela " + table);
            int rows = (10 + new Random().nextInt(990)) * 1000;
            messages.add("Tabela " + table + " contém aproximadamente " + rows + " linhas");
            messages.add("Salvando estrutura da tabela " + table);
            messages.add("Exportando dados da tabela " + table);
            messages.add("Dados da tabela " + table + " exportados com sucesso");
        }

        // Finalizando
        messages.add("Exportando índices e chaves estrangeiras");
        messages.add("Exportando procedimentos armazenados e funções");
        messages.add("Exportando triggers e views");
        messages.add("Compactando arquivo de backup");
        messages.add("Calculando checksum do arquivo de backup");

        return messages;
    }

    // Criar uma lista de mensagens realistas para restore
    private List<String> createRestoreMessages(String dbName) {
        List<String> messages = new ArrayList<>();

        // Configuração
        messages.add("Preparando ambiente para restore em " + dbName);
        messages.add("Verificando arquivo de backup");
        messages.add("Verificando permissões no banco de dados destino");
        messages.add("Iniciando processo de restore no banco " + dbName);

        // Descompressão
        messages.add("Descompactando arquivo de backup");
        for (int i = 10; i <= 100; i += 10) {
            messages.add("Descompressão em andamento: " + i + "%");
        }
        messages.add("Arquivo descompactado com sucesso");

        // Processo de restauração
        messages.add("Analisando estrutura do arquivo de backup");
        messages.add("Criando estrutura do banco de dados");

        // Adicionar mensagens por tabela
        String[] tables = {"users", "accounts", "transactions", "logs", "configurations", "reports", "events"};
        for (String table : tables) {
            messages.add("Criando tabela " + table);
            messages.add("Tabela " + table + " criada com sucesso");
            messages.add("Restaurando dados da tabela " + table);
            int rows = (10 + new Random().nextInt(990)) * 1000;
            messages.add("Inserindo aproximadamente " + rows + " linhas na tabela " + table);
            messages.add("Dados restaurados na tabela " + table);
        }

        // Finalizando
        messages.add("Restaurando índices e chaves estrangeiras");
        messages.add("Restaurando procedimentos armazenados e funções");
        messages.add("Restaurando triggers e views");
        messages.add("Reconstruindo índices");
        messages.add("Atualizando estatísticas do banco de dados");

        return messages;
    }

    private void shutdownExecutor(ExecutorService executor) {
        try {
            // Primeiro tenta desligar normalmente
            executor.shutdown();

            // Espera um pouco para que as tarefas terminem
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                // Força o encerramento após 5 segundos
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Restaura o flag de interrupção
            Thread.currentThread().interrupt();
            // Força o encerramento em caso de interrupção
            executor.shutdownNow();
        } catch (Exception e) {
            // Força o encerramento em caso de qualquer outro erro
            executor.shutdownNow();
        }
    }
}
