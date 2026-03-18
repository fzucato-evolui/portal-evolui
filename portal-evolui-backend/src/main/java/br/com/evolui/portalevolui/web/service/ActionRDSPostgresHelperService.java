package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.listener.ProgressStatusListener;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BucketDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSAccountConfigDTO;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

@Service
public class ActionRDSPostgresHelperService extends ActionRDSHelperService {

    private static final int CHUNK_SIZE = 64 * 1024 * 1024; // 64 MiB
    @Override
    public LinkedHashSet<String> retrieveRDSSchemas(RDSDTO rds) throws SQLException {
        LinkedHashSet<String> schemas = new LinkedHashSet<>();
        try (Connection connection = this.getConnection(rds);
             PreparedStatement st = connection.prepareStatement("SELECT datname FROM pg_database where datistemplate=false ORDER BY datname ASC;")) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString("datname"));
                }
            }
            return schemas;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public LinkedHashSet<String> retrieveRDSTablespaces(RDSDTO dto) throws SQLException {
        return null;
    }

    @Override
    public Future<BackupRestoreRDSDTO> engineBackup(final BackupRestoreRDSDTO dto) {
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            AWSService service = null;
            try {
                List<String> comando = new ArrayList<>();
                comando.add("pg_dump");
                if (bean.getExcludeBlobs()) {
                    comando.add("--no-blobs");
                }
                comando.add("--verbose");
                comando.add("-h");
                comando.add(rds.getEndpoint());
                comando.add("-U");
                comando.add(rds.getUsername());
                comando.add("-d");
                comando.add(bean.getSourceDatabase());
                comando.add("-F");
                comando.add("c");

                ProcessBuilder pb = new ProcessBuilder(comando);
                Map<String, String> env = pb.environment();
                env.put("PGPASSWORD", rds.getPassword()); // Para evitar prompt interativo
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("Iniciando utilitário dump com o comando: %s", String.join(" ", comando)), Level.INFO, this.getClass(), true));
                Process processo = pb.start();

                InputStream pgDumpStream = processo.getInputStream(); // Esse é o stdout do processo
                InputStream stderrStream = processo.getErrorStream(); // stderr

                service = this.getService();
                AtomicBoolean canceled = new AtomicBoolean(false);
                service.initialize(bean.getDumpFile().getAccount());
                Future<?> req = service.uploadHugeStreamToS3(bean.getDumpFile(), pgDumpStream, CHUNK_SIZE, new ProgressStatusListener() {
                    @Override
                    public void onProgress(double percentage) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Enviados " + percentage + " bytes", Level.INFO, this.getClass(), true));
                    }

                    @Override
                    public void onTotalBytes(long totalBytes) {

                    }

                    @Override
                    public void onReadBytes(byte[] readBytes, int length) {
//                        Tirei pq o chunk fica gigantesco e fica quase ilegível no log, além de travar a tela de status
//                        String line = new String(readBytes, 0, length);
//                        if (line.toUpperCase().contains("ERROR") || line.toUpperCase().contains("FATAL") || line.toUpperCase().contains("FAIL")) {
//                            dto.addStatus(new BackupRestoreRDSStatusDTO("Erro detectado no dump: " + line, Level.ERROR, this.getClass(), true));
//                        } else {
//                            dto.addStatus(new BackupRestoreRDSStatusDTO("Saída do dump: " + line, Level.DEBUG, this.getClass(), true));
//                        }
                    }
                }, canceled);
                List<String> stdoutLines = Collections.synchronizedList(new ArrayList<>());
                List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

                //Thread stdoutThread = new Thread(() -> readStream(dto, processo.getInputStream(), stdoutLines, false));
                Thread stderrThread = new Thread(() -> readStream(dto, processo.getErrorStream(), stderrLines, true));

                //stdoutThread.start();
                stderrThread.start();
                long pid = processo.pid();
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("PID do processo : %s", pid), Level.INFO, this.getClass(), true));


                try {
                    while (processo.isAlive() || !req.isDone()) {
                        if (dto.isCanceled()) {
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando backup...", Level.WARN, this.getClass(), true));
                            canceled.set(true);
                            req.cancel(true);
                            //stdoutThread.interrupt();
                            stderrThread.interrupt();
                            processo.destroyForcibly();
                            throw new InterruptedException("Backup cancelado pelo usuário.");
                        }
                        try {
                            Thread.sleep(1000); // Aguarda 1 segundo antes de verificar novamente
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restaura o estado de interrupção
                            //stdoutThread.interrupt();
                            stderrThread.interrupt();
                            processo.destroyForcibly();
                            throw new InterruptedException("Backup cancelado pelo usuário.");
                        }
                    }
                }
                finally {
                    if (pgDumpStream != null) {
                        try {
                            pgDumpStream.close();
                        } catch (IOException e) {
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao fechar pgDumpStream: " + e.getMessage(), Level.WARN, this.getClass(), true));
                        }
                    }
                }

                req.get();
                int exitCode = processo.waitFor();

                dto.addStatus(new BackupRestoreRDSStatusDTO("Garantindo que threads de leitura sejam interrompidas...", Level.DEBUG, this.getClass(), true));
                //stdoutThread.interrupt();
                stderrThread.interrupt();

                try {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Aguardando threads de leitura terminarem (máx 10s cada)...", Level.DEBUG, this.getClass(), true));
                    //stdoutThread.join(10000);
                    stderrThread.join(10000);

//                    if (stdoutThread.isAlive()) {
//                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stdout ainda está viva após timeout", Level.WARN, this.getClass(), true));
//                    }
                    if (stderrThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stderr ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Thread principal foi interrompida ao aguardar threads de leitura", Level.WARN, this.getClass(), true));
                }

                if (exitCode != 0) {
                    throw new RuntimeException(String.format("Backup falhou com código de saída %d. Erro: %s",
                            exitCode, String.join("\n", stderrLines).trim()));
                } else {
                    //System.out.println("Saída do comando:");
                    //stdoutLines.forEach(System.out::println);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup finalizado com sucesso!", Level.INFO, this.getClass(), true));

            } catch (Throwable e) {
                error = e; // Armazena o erro para relançar após a limpeza
                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro durante backup: " + e.getMessage(), Level.ERROR, this.getClass(), true));
            } finally {
                if (service != null) {
                    service.dispose();
                }
                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }

    //@Override
    public Future<BackupRestoreRDSDTO> engineBackupOld(final BackupRestoreRDSDTO dto) {
        AWSAccountConfigDTO config = this.getMainAccountConfig();
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();
        if (config == null || rds == null) {
            throw new RuntimeException("Configuração inválida");
        }
        if (!StringUtils.hasText(config.getBucketTempDump())) {
            throw new RuntimeException("Bucket de backup não configurado");
        }
        if (!StringUtils.hasText(config.getBucketLocalMountPath())) {
            throw new RuntimeException("Pasta de montagem do bucket não configurada");
        }

        String uuidPrefix = UUID.randomUUID().toString();
        String destinationFile = Paths.get(config.getBucketLocalMountPath(), String.format("%s_%s", uuidPrefix, bean.getDumpFile().getName())).toString();

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            try {
                List<String> comando = new ArrayList<>();
                comando.add("pg_dump");
                if (bean.getExcludeBlobs()) {
                    comando.add("--no-blobs");
                }
                comando.add("--verbose");
                comando.add("-h");
                comando.add(rds.getEndpoint());
                comando.add("-U");
                comando.add(rds.getUsername());
                comando.add("-d");
                comando.add(bean.getSourceDatabase());
                comando.add("-n");
                comando.add("public");
                comando.add("-F");
                comando.add("c");
                comando.add("-f");
                comando.add(destinationFile);

                ProcessBuilder pb = new ProcessBuilder(comando);
                Map<String, String> env = pb.environment();
                env.put("PGPASSWORD", rds.getPassword()); // Para evitar prompt interativo
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("Iniciando utilitário dump com o comando: %s", String.join(" ", comando)), Level.INFO, this.getClass(), true));
                Process processo = pb.start();

                List<String> stdoutLines = Collections.synchronizedList(new ArrayList<>());
                List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

                Thread stdoutThread = new Thread(() -> readStream(dto, processo.getInputStream(), stdoutLines, false));
                Thread stderrThread = new Thread(() -> readStream(dto, processo.getErrorStream(), stderrLines, true));

                stdoutThread.start();
                stderrThread.start();
                long pid = processo.pid();
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("PID do processo : %s", pid), Level.INFO, this.getClass(), true));

                while (processo.isAlive()) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando backup...", Level.WARN, this.getClass(), true));
                        stdoutThread.interrupt();
                        stderrThread.interrupt();
                        processo.destroyForcibly();
                        throw new InterruptedException("Backup cancelado pelo usuário.");
                    }
                    try {
                        Thread.sleep(1000); // Aguarda 1 segundo antes de verificar novamente
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restaura o estado de interrupção
                        stdoutThread.interrupt();
                        stderrThread.interrupt();
                        processo.destroyForcibly();
                        throw new InterruptedException("Backup cancelado pelo usuário.");
                    }
                }

                int exitCode = processo.waitFor();

                dto.addStatus(new BackupRestoreRDSStatusDTO("Garantindo que threads de leitura sejam interrompidas...", Level.DEBUG, this.getClass(), true));
                stdoutThread.interrupt();
                stderrThread.interrupt();

                try {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Aguardando threads de leitura terminarem (máx 10s cada)...", Level.DEBUG, this.getClass(), true));
                    stdoutThread.join(10000);
                    stderrThread.join(10000);

                    if (stdoutThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stdout ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                    if (stderrThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stderr ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Thread principal foi interrompida ao aguardar threads de leitura", Level.WARN, this.getClass(), true));
                }

                if (exitCode != 0) {
                    throw new RuntimeException(String.format("Backup falhou com código de saída %d. Erro: %s",
                            exitCode, String.join("\n", stderrLines).trim()));
                } else {
                    //System.out.println("Saída do comando:");
                    //stdoutLines.forEach(System.out::println);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup finalizado com sucesso!", Level.INFO, this.getClass(), true));

                moveFileToS3(dto, bean.getDumpFile(), destinationFile);

            } catch (Throwable e) {
                error = e; // Armazena o erro para relançar após a limpeza
                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro durante backup: " + e.getMessage(), Level.ERROR, this.getClass(), true));
            } finally {
                try {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Aguardando 5s para garantir liberação de recursos e sincronização...", Level.DEBUG, this.getClass(), true));
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                File dumpFile = new File(destinationFile);
                if (dumpFile.exists() && dumpFile.delete()) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo de dump deletado após erro ou sucesso: " + destinationFile, Level.DEBUG, this.getClass(), true));
                }

                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }

    public Future<BackupRestoreRDSDTO> engineRestore(final BackupRestoreRDSDTO dto) {
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();

        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            AWSService service = null;
            try {

                if (retrieveRDSSchemas(rds).contains(bean.getDestinationDatabase())) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados já existe. Removendo: " + bean.getDestinationDatabase(), Level.DEBUG, this.getClass(), true));
                    // Tentando desconectar outras sessões do banco
                    try (Connection connection = this.getConnection(rds);
                         PreparedStatement st = connection.prepareStatement(
                                 "SELECT pid FROM pg_stat_activity WHERE datname = ? AND pid <> pg_backend_pid();"
                         )) {
                        st.setString(1, bean.getDestinationDatabase());
                        ResultSet rs = st.executeQuery();
                        while (rs.next()) {
                            int pid = rs.getInt("pid");
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " desconectada.", Level.DEBUG, this.getClass(), true));

                            String terminateSessionQuery = "SELECT pg_terminate_backend(" + pid + ")";
                            try (Statement terminateStatement = connection.createStatement()) {
                                terminateStatement.execute(terminateSessionQuery);
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " foi terminada.", Level.DEBUG, this.getClass(), true));
                            } catch (SQLException e) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar terminar a sessão com PID " + pid + ": " + e.getMessage(), Level.WARN, this.getClass(), true));
                            }
                        }
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar desconectar sessões do banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }

                    String dropQuery = "DROP DATABASE IF EXISTS " + quotePgIdentifier(bean.getDestinationDatabase());
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        st.executeUpdate(dropQuery);
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados removido com sucesso.", Level.DEBUG, this.getClass(), true));
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar remover o banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }
                }

                try (Connection connection = this.getConnection(rds);
                     Statement st = connection.createStatement()) {
                    st.executeUpdate("CREATE DATABASE " + quotePgIdentifier(bean.getDestinationDatabase()));
                }

                if (dto.isCanceled()) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                    throw new InterruptedException("Restore cancelado pelo usuário.");
                }

                boolean databaseReady = false;
                int retries = 0;
                while (!databaseReady && retries < 10) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                        throw new InterruptedException("Restore cancelado pelo usuário.");
                    }
                    try (Connection connection = this.getConnection(rds);
                         PreparedStatement st = connection.prepareStatement(
                                 "SELECT 1 FROM pg_database WHERE datname = ? LIMIT 1"
                         )) {
                        st.setString(1, bean.getDestinationDatabase());
                        st.executeQuery();
                        databaseReady = true;
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados ainda não está pronto, aguardando...", Level.DEBUG, this.getClass(), true));
                        retries++;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!databaseReady) {
                    throw new RuntimeException("Banco de dados de destino não está disponível após várias tentativas.");
                }

                service = this.getService();
                service.initialize(bean.getDumpFile().getAccount());
                try (InputStream rawStream = service.getResumableFileStreamFromS3(bean.getDumpFile(), 10, 5000)) {

                    if (rawStream == null) {
                        throw new FileNotFoundException("Arquivo não encontrado no S3: " + bean.getDumpFile().getKey());
                    }

                    BufferedInputStream buffered = new BufferedInputStream(rawStream, 1_048_576); // 1MB buffer S3

                    // Detecta GZIP pelos magic bytes (0x1F 0x8B)
                    boolean isGzip = detectGzip(buffered);
                    dto.addStatus(new BackupRestoreRDSStatusDTO(
                            isGzip ? "Formato detectado: arquivo GZIP comprimido" : "Formato detectado: arquivo não comprimido",
                            Level.INFO, this.getClass(), true));

                    // Se GZIP, descomprime com buffers maiores para reduzir leituras no S3
                    InputStream dataStream = isGzip
                            ? new BufferedInputStream(new GZIPInputStream(buffered, 65536), 1_048_576)
                            : buffered;

                    // Detecta PostgreSQL custom dump pelos magic bytes (PGDMP)
                    boolean isCustomFormat = detectPgCustomFormat(dataStream);
                    dto.addStatus(new BackupRestoreRDSStatusDTO(
                            isCustomFormat
                                    ? "Formato de dump detectado: PostgreSQL custom format (pg_restore)"
                                    : "Formato de dump detectado: SQL plain text (psql)",
                            Level.INFO, this.getClass(), true));

                    List<String> comando;
                    if (isCustomFormat) {
                        comando = new ArrayList<>();
                        comando.add("pg_restore");
                        if (bean.getExcludeBlobs()) {
                            comando.add("--no-blobs");
                        }
                        comando.add("--verbose");
                        comando.add("--clean");
                        comando.add("--if-exists");
                        comando.add("--no-comments");
                        comando.add("--no-owner");
                        comando.add("--no-privileges");
                        comando.add("-h");
                        comando.add(rds.getEndpoint());
                        comando.add("-U");
                        comando.add(rds.getUsername());
                        comando.add("-d");
                        comando.add(bean.getDestinationDatabase());
                    } else {
                        comando = Arrays.asList(
                                "psql",
                                "--echo-all",
                                "--echo-errors",
                                "-h", rds.getEndpoint(),
                                "-U", rds.getUsername(),
                                "-d", bean.getDestinationDatabase()
                        );
                    }

                    ProcessBuilder pb = new ProcessBuilder(comando);
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD); // stdout não é usado; descartar para evitar deadlock

                    Map<String, String> env = pb.environment();
                    env.put("PGPASSWORD", rds.getPassword());
                    dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("Iniciando utilitário restore com o comando: %s", String.join(" ", comando)), Level.INFO, this.getClass(), true));
                    Process processo = pb.start();

                    List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());
                    Thread stderrThread = new Thread(() -> readStream(dto, processo.getErrorStream(), stderrLines, true));
                    stderrThread.start();
                    long pid = processo.pid();
                    dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("PID do processo : %s", pid), Level.INFO, this.getClass(), true));

                    // Thread intermediária: lê do S3/GZIP e alimenta um pipe com buffer grande.
                    // Isso desacopla a velocidade de leitura do S3 da velocidade de consumo do pg_restore,
                    // evitando que a conexão S3 fique ociosa e seja resetada.
                    PipedOutputStream pipedOut = new PipedOutputStream();
                    PipedInputStream pipedIn = new PipedInputStream(pipedOut, 16 * 1024 * 1024); // 16MB buffer

                    final InputStream s3Data = dataStream;
                    Thread s3ReaderThread = new Thread(() -> {
                        try {
                            byte[] buf = new byte[64 * 1024];
                            int n;
                            while ((n = s3Data.read(buf)) != -1) {
                                pipedOut.write(buf, 0, n);
                            }
                        } catch (IOException e) {
                            dto.addStatus(new BackupRestoreRDSStatusDTO(
                                    "Leitura do stream S3 encerrada: " + e.getMessage(),
                                    Level.WARN, this.getClass(), true));
                        } finally {
                            try { pipedOut.close(); } catch (IOException ignored) {}
                        }
                    }, "s3-reader-thread");
                    s3ReaderThread.setDaemon(true);
                    s3ReaderThread.start();

                    // Lê do pipe intermediário e escreve no stdin do pg_restore/psql
                    try (OutputStream stdin = processo.getOutputStream()) {
                        byte[] buffer = new byte[64 * 1024];
                        int bytesRead;
                        while ((bytesRead = pipedIn.read(buffer)) != -1) {
                            try {
                                stdin.write(buffer, 0, bytesRead);
                                stdin.flush();
                            } catch (IOException pipeEx) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO(
                                        "Pipe encerrado pelo processo (provavelmente já finalizou). Interrompendo envio de dados.",
                                        Level.WARN, this.getClass(), true));
                                break;
                            }
                            if (dto.isCanceled()) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                                stderrThread.interrupt();
                                processo.destroyForcibly();
                                throw new InterruptedException("Restore cancelado pelo usuário.");
                            }
                        }
                    } finally {
                        pipedIn.close();
                    }

                    s3ReaderThread.join(30000);
                    if (s3ReaderThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread de leitura S3 ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }

                int exitCode = processo.waitFor();

                stderrThread.interrupt();
                try {
                    stderrThread.join(10000);
                    if (stderrThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stderr ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                boolean hasOnlyIgnorableWarnings = stderrLines.isEmpty() || stderrLines.stream().allMatch(line ->
                        line.contains("unrecognized configuration parameter") ||
                                line.contains("errors ignored on restore"));

                if (exitCode != 0) {
                    if (hasOnlyIgnorableWarnings) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Restore finalizado com warnings conhecidos e ignoráveis.", Level.WARN, this.getClass(), true));
                        dto.setWarning(String.join("\n", stderrLines).trim());
                    }
                    else {
                        throw new RuntimeException(String.format("Restore falhou com código de saída %d. Erro: %s",
                                exitCode, String.join("\n", stderrLines).trim()));
                    }
                }
                else {
                    //System.out.println("Saída do comando:");
                    //stdoutLines.forEach(System.out::println);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restauração finalizada com sucesso!", Level.INFO, this.getClass(), true));
                } // fecha try-with-resources do rawStream

            } catch (Throwable e) {
                e.printStackTrace();
                error = e;
            } finally {
                if (service != null) {
                    try {
                        service.dispose();
                    } catch (Exception e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao liberar recursos do serviço: " + e.getMessage(), Level.ERROR, this.getClass(), true));
                    }
                }
                // Se houver erro, relança
                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }
    //@Override
    public Future<BackupRestoreRDSDTO> engineRestoreOld(final BackupRestoreRDSDTO dto) {
        AWSAccountConfigDTO config = this.getMainAccountConfig();
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();
        if (config == null || rds == null) {
            throw new RuntimeException("Configuração inválida");
        }
        if (!StringUtils.hasText(config.getBucketLocalMountPath())) {
            throw new RuntimeException("Pasta de montagem do bucket não configurada");
        }

        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            String sourceFile = null;
            Path decompressedFolder = null;
            try {
                sourceFile = Paths.get(config.getBucketLocalMountPath(), bean.getDumpFile().getName()).toString();
                downloadFileFromS3(dto, bean.getDumpFile(), sourceFile);

                // Verifica se o arquivo está compactado e descomprime
                if (checkCompressedFile(dto, Paths.get(sourceFile))) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo compactado detectado. Descomprimindo: " + sourceFile, Level.DEBUG, this.getClass(), true));

                    // Gerar UUID para o arquivo descomprimido temporário
                    String uuidPrefix = UUID.randomUUID().toString();
                    decompressedFolder = Paths.get(config.getBucketLocalMountPath(), "decompressed_" + uuidPrefix);
                    if (!Files.exists(decompressedFolder)) {
                        Files.createDirectories(decompressedFolder);
                    }
                    
                    // Obter o tamanho estimado do arquivo descomprimido
                    File compressedFile = new File(sourceFile);
                    long totalBytes = compressedFile.length(); // Tamanho do arquivo comprimido
                    long expectedUncompressedSize = getGzipUncompressedSize(compressedFile);
                    
                    // Se não conseguir obter o tamanho descomprimido, usamos o tamanho comprimido como fallback
                    long bytesToUseForProgress = expectedUncompressedSize > 0 ? expectedUncompressedSize : totalBytes;
                    
                    if (expectedUncompressedSize > 0) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Tamanho estimado após descompressão: " + 
                            formatFileSize(expectedUncompressedSize), Level.DEBUG, this.getClass(), true));
                    } else {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Não foi possível determinar o tamanho descomprimido. " + 
                            "Usando tamanho comprimido para estimativa de progresso.", Level.DEBUG, this.getClass(), true));
                    }
                    
                    long decompressedBytes = 0;

                    // Descompressão do arquivo usando um fluxo de entrada GZIP
                    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(sourceFile))) {
                        FileOutputStream fos = new FileOutputStream(Paths.get(decompressedFolder.toString(), "dump").toString());
                        byte[] buffer = new byte[8192];
                        int len;
                        final AtomicLong lastUpdate = new AtomicLong(0);
                        while ((len = gis.read(buffer)) > 0) {
                            if (dto.isCanceled()) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                                throw new InterruptedException("Restore cancelado pelo usuário.");
                            }
                            fos.write(buffer, 0, len);
                            decompressedBytes += len;
                            
                            // Calcular progresso baseado no tamanho real descomprimido ou estimado
                            double percent = Math.min(100.0, (decompressedBytes * 100.0) / bytesToUseForProgress);
                            
                            long now = System.currentTimeMillis();
                            long previous = lastUpdate.get();
                            if (now - previous >= 10000) { // 10 segundos = 10000 ms
                                lastUpdate.set(now);
                                dto.addStatus(new BackupRestoreRDSStatusDTO(
                                    String.format("Descompressão em andamento: %.2f%% (%s de %s)", 
                                    percent, 
                                    formatFileSize(decompressedBytes),
                                    formatFileSize(bytesToUseForProgress)),
                                    Level.DEBUG, this.getClass(), true));
                            }
                        }
                        fos.close();
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Descompressão concluída com sucesso!", Level.DEBUG, this.getClass(), true));
                    } catch (IOException e) {
                        throw new RuntimeException("Erro ao descomprimir o arquivo: " + e.getMessage(), e);
                    }

                    // Remover ou substituir o arquivo comprimido após a descompressão
                    if (compressedFile.exists() && compressedFile.delete()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo comprimido removido: " + sourceFile, Level.DEBUG, this.getClass(), true));
                    } else {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Falha ao remover o arquivo comprimido: " + sourceFile, Level.WARN, this.getClass(), true));
                    }

                    sourceFile = Paths.get(decompressedFolder.toString(), "dump").toString();
                } else {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo não está compactado, utilizando o arquivo original.", Level.DEBUG, this.getClass(), true));
                }
                if (retrieveRDSSchemas(rds).contains(bean.getDestinationDatabase())) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados já existe. Removendo: " + bean.getDestinationDatabase(), Level.DEBUG, this.getClass(), true));
                    // Tentando desconectar outras sessões do banco
                    String terminateQuery = "SELECT pid FROM pg_stat_activity WHERE datname = '" + bean.getDestinationDatabase() + "' AND pid <> pg_backend_pid();";
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        ResultSet rs = st.executeQuery(terminateQuery);
                        while (rs.next()) {
                            int pid = rs.getInt("pid"); // A coluna correta é "pid"
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " desconectada.", Level.DEBUG, this.getClass(), true));

                            String terminateSessionQuery = "SELECT pg_terminate_backend(" + pid + ")";
                            try (Statement terminateStatement = connection.createStatement()) {
                                terminateStatement.execute(terminateSessionQuery); // Não precisa capturar resultado
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " foi terminada.", Level.DEBUG, this.getClass(), true));
                            } catch (SQLException e) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar terminar a sessão com PID " + pid + ": " + e.getMessage(), Level.WARN, this.getClass(), true));
                            }
                        }
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar desconectar sessões do banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }

                    String dropQuery = "DROP DATABASE IF EXISTS " + bean.getDestinationDatabase();
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        st.executeUpdate(dropQuery);
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados removido com sucesso.", Level.DEBUG, this.getClass(), true));
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar remover o banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }
                }

                try (Connection connection = this.getConnection(rds);
                     Statement st = connection.createStatement()) {
                    st.executeUpdate("CREATE DATABASE " + bean.getDestinationDatabase());
                }

                if (dto.isCanceled()) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                    throw new InterruptedException("Restore cancelado pelo usuário.");
                }

                boolean databaseReady = false;
                int retries = 0;
                while (!databaseReady && retries < 10) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                        throw new InterruptedException("Restore cancelado pelo usuário.");
                    }
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + bean.getDestinationDatabase() + "' LIMIT 1;");
                        databaseReady = true;
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados ainda não está pronto, aguardando...", Level.DEBUG, this.getClass(), true));
                        retries++;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!databaseReady) {
                    throw new RuntimeException("Banco de dados de destino não está disponível após várias tentativas.");
                }

                List<String> comando;

                if (isCustomDump(sourceFile)) {
                    comando = new ArrayList<>();
                    comando.add("pg_restore");
                    if (bean.getExcludeBlobs()) {
                        comando.add("--no-blobs");
                    }
                    comando.add("--verbose");
                    comando.add("-h");
                    comando.add(rds.getEndpoint());
                    comando.add("-U");
                    comando.add(rds.getUsername());
                    comando.add("-d");
                    comando.add(bean.getDestinationDatabase());
                    comando.add("-n");
                    comando.add("public");
                    comando.add("-F");
                    comando.add("c");
                    comando.add(sourceFile);
                } else {
                    comando = Arrays.asList(
                            "psql",
                            "--verbose",
                            "-h", rds.getEndpoint(),
                            "-U", rds.getUsername(),
                            "-d", bean.getDestinationDatabase(),
                            "-f", sourceFile
                    );
                }

                ProcessBuilder pb = new ProcessBuilder(comando);
                //pb.redirectErrorStream(true); // Une stdout e stderr

                Map<String, String> env = pb.environment();
                env.put("PGPASSWORD", rds.getPassword()); // Evita prompt por senha
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("Iniciando utilitário restore com o comando: %s", String.join(" ", comando)), Level.INFO, this.getClass(), true));
                Process processo = pb.start();

                List<String> stdoutLines = Collections.synchronizedList(new ArrayList<>());
                List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

                Thread stdoutThread = new Thread(() -> readStream(dto, processo.getInputStream(), stdoutLines, false));
                Thread stderrThread = new Thread(() -> readStream(dto, processo.getErrorStream(), stderrLines, true));

                stdoutThread.start();
                stderrThread.start();
                long pid = processo.pid();
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("PID do processo : %s", pid), Level.INFO, this.getClass(), true));

                while (processo.isAlive()) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                        stdoutThread.interrupt();
                        stderrThread.interrupt();
                        processo.destroyForcibly();
                        throw new InterruptedException("Restore cancelado pelo usuário.");
                    }
                    try {
                        Thread.sleep(1000); // Aguarda 1 segundo antes de verificar novamente
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restaura o estado de interrupção
                        stdoutThread.interrupt();
                        stderrThread.interrupt();
                        processo.destroyForcibly();
                        throw new InterruptedException("Backup cancelado pelo usuário.");
                    }
                }
                int exitCode = processo.waitFor();

                dto.addStatus(new BackupRestoreRDSStatusDTO("Garantindo que threads de leitura sejam interrompidas...", Level.DEBUG, this.getClass(), true));
                stdoutThread.interrupt();
                stderrThread.interrupt();

                try {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Aguardando threads de leitura terminarem (máx 10s cada)...", Level.DEBUG, this.getClass(), true));
                    stdoutThread.join(10000);
                    stderrThread.join(10000);

                    if (stdoutThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stdout ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                    if (stderrThread.isAlive()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("AVISO: Thread stderr ainda está viva após timeout", Level.WARN, this.getClass(), true));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Thread principal foi interrompida ao aguardar threads de leitura", Level.WARN, this.getClass(), true));
                }

                if (exitCode != 0) {
                    throw new RuntimeException(String.format("Restore falhou com código de saída %d. Erro: %s",
                            exitCode, String.join("\n", stderrLines).trim()));
                } else {
                    //System.out.println("Saída do comando:");
                    //stdoutLines.forEach(System.out::println);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restauração finalizada com sucesso!", Level.INFO, this.getClass(), true));

            } catch (Throwable e) {
                error = e;
            } finally {
                try {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Aguardando 5s para garantir liberação de recursos e sincronização...", Level.DEBUG, this.getClass(), true));
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Limpeza de arquivos e pastas criados
                if (sourceFile != null) {
                    File dumpFile = new File(sourceFile);
                    if (dumpFile.exists() && dumpFile.delete()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo de dump deletado após erro ou sucesso: " + sourceFile, Level.DEBUG, this.getClass(), true));
                    } else {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Falha ao deletar arquivo de dump: " + sourceFile, Level.WARN, this.getClass(), true));
                    }
                }

                // Limpeza do diretório de descompressão
                if (decompressedFolder != null && Files.exists(decompressedFolder)) {
                    try {
                        // Remove arquivos e subdiretórios
                        Files.walk(decompressedFolder)
                                .sorted(Comparator.reverseOrder()) // Começa pelos arquivos mais profundos
                                .map(Path::toFile)
                                .forEach(file -> {
                                    if (file.delete()) {
                                        dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo ou diretório deletado: " + file.getPath(), Level.DEBUG, this.getClass(), true));
                                    } else {
                                        dto.addStatus(new BackupRestoreRDSStatusDTO("Falha ao deletar arquivo ou diretório: " + file.getPath(), Level.WARN, this.getClass(), true));
                                    }
                                });

                        // Apaga o diretório vazio após limpar
                        if (Files.exists(decompressedFolder)) {
                            Files.delete(decompressedFolder);
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Diretório de descompressão removido após erro ou sucesso: " + decompressedFolder, Level.DEBUG, this.getClass(), true));
                        } else {
                            dto.addStatus(new BackupRestoreRDSStatusDTO("O diretório de descompressão não existe mais: " + decompressedFolder, Level.DEBUG, this.getClass(), true));
                        }
                    } catch (IOException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao remover diretório de descompressão: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }
                }
                // Se houver erro, relança
                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }

    public Future<BackupRestoreRDSDTO> engineRestorePeloTeste(final BackupRestoreRDSDTO dto, String filePath) {
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();

        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            String sourceFile = null;
            Path decompressedFolder = null;
            try {
                sourceFile = filePath;
                if (retrieveRDSSchemas(rds).contains(bean.getDestinationDatabase())) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados já existe. Removendo: " + bean.getDestinationDatabase(), Level.DEBUG, this.getClass(), true));
                    // Tentando desconectar outras sessões do banco
                    String terminateQuery = "SELECT pid FROM pg_stat_activity WHERE datname = '" + bean.getDestinationDatabase() + "' AND pid <> pg_backend_pid();";
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        ResultSet rs = st.executeQuery(terminateQuery);
                        while (rs.next()) {
                            int pid = rs.getInt("pid"); // A coluna correta é "pid"
                            dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " desconectada.", Level.DEBUG, this.getClass(), true));

                            String terminateSessionQuery = "SELECT pg_terminate_backend(" + pid + ")";
                            try (Statement terminateStatement = connection.createStatement()) {
                                terminateStatement.execute(terminateSessionQuery); // Não precisa capturar resultado
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Sessão com PID " + pid + " foi terminada.", Level.DEBUG, this.getClass(), true));
                            } catch (SQLException e) {
                                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar terminar a sessão com PID " + pid + ": " + e.getMessage(), Level.WARN, this.getClass(), true));
                            }
                        }
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar desconectar sessões do banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }

                    String dropQuery = "DROP DATABASE IF EXISTS " + bean.getDestinationDatabase();
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        st.executeUpdate(dropQuery);
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados removido com sucesso.", Level.DEBUG, this.getClass(), true));
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao tentar remover o banco de dados: " + e.getMessage(), Level.WARN, this.getClass(), true));
                    }
                }

                try (Connection connection = this.getConnection(rds);
                     Statement st = connection.createStatement()) {
                    st.executeUpdate("CREATE DATABASE " + bean.getDestinationDatabase());
                }

                if (dto.isCanceled()) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                    throw new InterruptedException("Restore cancelado pelo usuário.");
                }

                boolean databaseReady = false;
                int retries = 0;
                while (!databaseReady && retries < 10) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                        throw new InterruptedException("Restore cancelado pelo usuário.");
                    }
                    try (Connection connection = this.getConnection(rds);
                         Statement st = connection.createStatement()) {
                        st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + bean.getDestinationDatabase() + "' LIMIT 1;");
                        databaseReady = true;
                    } catch (SQLException e) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados ainda não está pronto, aguardando...", Level.DEBUG, this.getClass(), true));
                        retries++;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!databaseReady) {
                    throw new RuntimeException("Banco de dados de destino não está disponível após várias tentativas.");
                }

                String comando;
                if (isCustomDump(sourceFile)) {
                    comando = String.format(
                            "pg_restore %s -h %s -U %s -d %s -n public -F c %s",
                            bean.getExcludeBlobs() ? "--no-blobs" : "",
                            rds.getEndpoint(), rds.getUsername(), bean.getDestinationDatabase(), sourceFile
                    );
                } else {
                    comando = String.format(
                            "psql -h %s -U %s -d %s -f %s",
                            rds.getEndpoint(), rds.getUsername(), bean.getDestinationDatabase(), sourceFile
                    );
                }

                ProcessBuilder pb;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    pb = new ProcessBuilder("cmd.exe", "/c", comando);
                } else {
                    pb = new ProcessBuilder("bash", "-c", comando);
                }

                Map<String, String> env = pb.environment();
                env.put("PGPASSWORD", rds.getPassword());

                Process processo = pb.start();

                List<String> stdoutLines = Collections.synchronizedList(new ArrayList<>());
                List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

                Thread stdoutThread = new Thread(() -> readStream(dto, processo.getInputStream(), stdoutLines, false));
                Thread stderrThread = new Thread(() -> readStream(dto, processo.getErrorStream(), stderrLines, true));

                stdoutThread.start();
                stderrThread.start();

                while (processo.isAlive()) {
                    if (dto.isCanceled()) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando restore...", Level.WARN, this.getClass(), true));
                        processo.destroyForcibly();
                        throw new InterruptedException("Restore cancelado pelo usuário.");
                    }
                }
                int exitCode = processo.waitFor();

                stdoutThread.interrupt();
                stderrThread.interrupt();

                try {
                    stdoutThread.join();
                    stderrThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (exitCode != 0) {
                    throw new RuntimeException(String.format("Restore falhou com código de saída %d. Erro: %s",
                            exitCode, String.join("\n", stderrLines).trim()));
                } else {
                    //System.out.println("Saída do comando:");
                    //stdoutLines.forEach(System.out::println);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restauração finalizada com sucesso!", Level.INFO, this.getClass(), true));

            } catch (Throwable e) {
                error = e;
            } finally {
                // Se houver erro, relança
                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }

    private Connection getConnection(RDSDTO rds) throws SQLException {
        String url = String.format("jdbc:postgresql://%s/%s", rds.getEndpoint(), "postgres");
        return DriverManager.getConnection(url, rds.getUsername(), rds.getPassword());
    }
    
    /**
     * Obtém o tamanho descomprimido de um arquivo GZIP lendo os últimos 4 bytes
     * @param file O arquivo GZIP
     * @return O tamanho descomprimido ou -1 se não for possível determinar
     */
    private long getGzipUncompressedSize(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Verifica se o arquivo tem pelo menos 4 bytes
            if (raf.length() < 4) {
                return -1;
            }
            
            // Os últimos 4 bytes contêm o tamanho original em formato little-endian
            raf.seek(raf.length() - 4);
            int b4 = raf.read() & 0xFF;
            int b3 = raf.read() & 0xFF;
            int b2 = raf.read() & 0xFF;
            int b1 = raf.read() & 0xFF;
            
            // Converter 4 bytes para um valor long (formato little-endian)
            long size = ((long) b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            
            // Se o tamanho parece muito grande ou muito pequeno, pode ser inválido
            if (size <= 0 || size > file.length() * 1000) { // Assumimos que a taxa de compressão não é maior que 1000x
                return -1;
            }
            
            return size;
        } catch (Exception e) {
            return -1; // Em caso de erro, retorna -1
        }
    }
    
    /**
     * Formata um tamanho de arquivo em bytes para uma representação legível
     * @param size Tamanho em bytes
     * @return String formatada (ex: "1.23 MB")
     */
    private String formatFileSize(long size) {
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double sizeAsDouble = size;
        
        while (sizeAsDouble >= 1024 && unitIndex < units.length - 1) {
            sizeAsDouble /= 1024;
            unitIndex++;
        }
        
        // Formatar com 2 casas decimais se não for bytes
        if (unitIndex == 0) {
            return String.format("%d %s", (long)sizeAsDouble, units[unitIndex]);
        } else {
            return String.format("%.2f %s", sizeAsDouble, units[unitIndex]);
        }
    }

    private AWSAccountConfigDTO getMainAccountConfig() {
        AWSService service = this.getService();
        try {
            return service.getConfig();
        }
        finally {
            service.dispose();
        }
    }

    private void moveFileToS3(BackupRestoreRDSDTO backupRestoreRDSDTO, BucketDTO dto, String filePath) throws Exception {
        AWSService service = this.getService();
        try {
            service.initialize(dto.getAccount());
            AtomicBoolean canceled = new AtomicBoolean(false);
            Future<?> req = service.uploadFileToS3(dto, filePath, new ProgressStatusListener() {
                @Override
                public void onProgress(double percentage) {
                    backupRestoreRDSDTO.addStatus(new BackupRestoreRDSStatusDTO("Upload em andamento: " + percentage + "%", Level.DEBUG, this.getClass(), true));
                }

                @Override
                public void onTotalBytes(long totalBytes) {

                }

                @Override
                public void onReadBytes(byte[] readBytes, int length) {

                }
            }, canceled);

            while (!req.isDone()) {
                if (backupRestoreRDSDTO.isCanceled()) {
                    backupRestoreRDSDTO.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando upload...", Level.WARN, this.getClass(), true));
                    canceled.set(true);
                    req.cancel(true);
                    break;
                }
                Thread.sleep(1000);
            }
            req.get();
            if (canceled.get()) {
                throw new InterruptedException("Upload cancelado pelo usuário.");
            }
        }
        finally {
            service.dispose();
        }
    }

    private void downloadFileFromS3(BackupRestoreRDSDTO backupRestoreRDSDTO, BucketDTO dto, String filePath) throws Exception {
        AWSService service = this.getService();
        try {
            service.initialize(dto.getAccount());
            AtomicBoolean canceled = new AtomicBoolean(false);
            final AtomicLong lastUpdate = new AtomicLong(0);
            Future<?> req = service.downloadFileFromS3(dto, filePath, new ProgressStatusListener() {
                @Override
                public void onProgress(double percentage) {
                    long now = System.currentTimeMillis();
                    long previous = lastUpdate.get();
                    if (now - previous >= 10000) { // 10 segundos = 10000 ms
                        lastUpdate.set(now);
                        backupRestoreRDSDTO.addStatus(
                                new BackupRestoreRDSStatusDTO("Download em andamento: " + percentage + "%", Level.DEBUG, this.getClass(), true)
                        );
                    }
                }

                @Override
                public void onTotalBytes(long totalBytes) {

                }

                @Override
                public void onReadBytes(byte[] readBytes, int length) {

                }
            }, canceled);

            while (!req.isDone()) {
                if (backupRestoreRDSDTO.isCanceled()) {
                    backupRestoreRDSDTO.addStatus(new BackupRestoreRDSStatusDTO("Cancelamento solicitado. Finalizando download...", Level.WARN, this.getClass(), true));
                    canceled.set(true);
                    req.cancel(true);
                    break;
                }
                Thread.sleep(1000);
            }
            req.get();
            if (canceled.get()) {
                throw new InterruptedException("Download cancelado pelo usuário.");
            }
        }
        finally {
            service.dispose();
        }
    }

    private boolean checkCompressedFile(BackupRestoreRDSDTO dto, Path filePath) {
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            dto.addStatus(new BackupRestoreRDSStatusDTO("Arquivo não encontrado ou sem permissão de leitura: " + filePath, Level.WARN, this.getClass(), true));
            return false;
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            new CompressorStreamFactory().createCompressorInputStream(is);
            return true; // Se não deu erro, é válido
        } catch (CompressorException e) {
            return false; // Não é o tipo esperado
        } catch (IOException e) {
            dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao ler o arquivo: " + e.getMessage(), Level.WARN, this.getClass(), true));
            return false;
        }
    }

    public boolean isCustomDump(String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            byte[] header = new byte[5];
            if (fis.read(header) == 5) {
                return new String(header).equals("PGDMP");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Detecta se o stream é GZIP verificando os magic bytes (0x1F 0x8B).
     * Usa mark/reset para não consumir os bytes do stream.
     */
    private boolean detectGzip(BufferedInputStream stream) throws IOException {
        stream.mark(2);
        try {
            int b1 = stream.read();
            int b2 = stream.read();
            return b1 == 0x1F && b2 == 0x8B;
        } finally {
            stream.reset();
        }
    }

    /**
     * Detecta se o stream é um dump PostgreSQL custom format verificando
     * o header "PGDMP" (5 bytes ASCII). Usa mark/reset para não consumir os bytes.
     */
    private boolean detectPgCustomFormat(InputStream stream) throws IOException {
        stream.mark(5);
        try {
            byte[] header = new byte[5];
            int read = 0;
            while (read < 5) {
                int r = stream.read(header, read, 5 - read);
                if (r == -1) return false;
                read += r;
            }
            return new String(header).equals("PGDMP");
        } finally {
            stream.reset();
        }
    }

    /**
     * Detecta se a linha é um erro real do PostgreSQL (pg_restore/pg_dump/psql),
     * baseado no formato de saída padrão, não em palavras soltas no conteúdo.
     *
     * Formatos reconhecidos (EN e PT-BR):
     *   pg_restore: error: ...  /  pg_restore: erro: ...
     *   pg_dump: error: ...     /  pg_dump: erro: ...
     *   ERROR:  ...             /  ERRO:  ...
     *   FATAL:  ...
     */
    private boolean isPgErrorLine(String line) {
        String lower = line.toLowerCase().trim();

        // pg_restore/pg_dump: prefixo "error:" ou "erro:" ou "fatal:" após o nome da ferramenta
        if (lower.startsWith("pg_restore: error:") || lower.startsWith("pg_restore: erro:") || lower.startsWith("pg_restore: fatal:") ||
            lower.startsWith("pg_dump: error:")    || lower.startsWith("pg_dump: erro:")    || lower.startsWith("pg_dump: fatal:")) {
            return true;
        }

        // psql: linha começa diretamente com o nível (ERROR: / ERRO: / FATAL:)
        if (lower.startsWith("error:") || lower.startsWith("erro:") || lower.startsWith("fatal:")) {
            return true;
        }

        // Frases específicas de erro contextual que não aparecem em nomes de objetos
        if (lower.contains("could not execute") ||
            lower.contains("could not connect") ||
            lower.contains("could not open") ||
            lower.contains("could not read from input file") ||
            lower.contains("permission denied") ||
            lower.contains("access denied") ||
            lower.contains("authentication failed")) {
            return true;
        }

        return false;
    }

    /**
     * Detecta se a linha é um warning do PostgreSQL.
     *
     * Formatos reconhecidos (EN e PT-BR):
     *   pg_restore: warning: ...
     *   pg_dump: warning: ...
     *   WARNING:  ...  /  AVISO:  ...
     */
    private boolean isPgWarningLine(String line) {
        String lower = line.toLowerCase().trim();

        if (lower.startsWith("pg_restore: warning:") || lower.startsWith("pg_dump: warning:") ||
            lower.startsWith("pg_restore: aviso:")   || lower.startsWith("pg_dump: aviso:")) {
            return true;
        }

        if (lower.startsWith("warning:") || lower.startsWith("aviso:") || lower.startsWith("notice:")) {
            return true;
        }

        return false;
    }

    private void readStream(BackupRestoreRDSDTO dto, InputStream inputStream, List<String> outputLines, boolean isStderr) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String linha;
            while (!Thread.currentThread().isInterrupted() && (linha = reader.readLine()) != null) {
                // Verifica novamente se foi interrompida antes de processar
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // Determina o nível de log baseado no formato de saída do PostgreSQL
                Level logLevel;
                if (isStderr) {
                    if (isPgErrorLine(linha)) {
                        logLevel = Level.ERROR;
                    } else if (isPgWarningLine(linha)) {
                        logLevel = Level.WARN;
                    } else {
                        logLevel = Level.INFO;
                    }
                    if (logLevel == Level.ERROR) {
                        outputLines.add(linha);
                    }
                } else {
                    logLevel = Level.DEBUG;
                    outputLines.add(linha);
                }

                dto.addStatus(new BackupRestoreRDSStatusDTO(linha, logLevel, this.getClass(), true));
            }
        } catch (IOException e) {
            // Se a thread foi interrompida, não registra erro
            if (!Thread.currentThread().isInterrupted()) {
                dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao ler stream: " + e.getMessage(), Level.WARN, this.getClass(), true));
            }
        } finally {
            // Garante o fechamento do reader
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignora erro ao fechar
                }
            }

            // Fecha o inputStream também
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignora erro ao fechar
            }
        }
    }

    private String quotePgIdentifier(String value) {
        if (value == null) {
            return null;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
