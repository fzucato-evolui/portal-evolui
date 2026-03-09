package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
public class ActionRDSSqlServerHelperService extends ActionRDSHelperService {

    @Override
    public LinkedHashSet<String> retrieveRDSSchemas(RDSDTO rds) throws SQLException {
        LinkedHashSet<String> schemas = new LinkedHashSet<>();
        try (Connection connection = this.getConnection(rds);
             Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT name FROM sys.databases WHERE state_desc = 'ONLINE' AND name != 'master' ORDER BY name ASC;")) {
                    while (rs.next()) {
                        schemas.add(rs.getString("name"));
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
    public Future<BackupRestoreRDSDTO> engineBackup(BackupRestoreRDSDTO dto) {
        ActionRDSBean bean = dto.getBean();
        String s3Arn = bean.getDumpFile().getArn();
        RDSDTO rds = bean.getRds();
        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            try (Connection connection = this.getConnection(rds);
                 Statement st = connection.createStatement()) {

                String backupQuery = String.format(
                        "EXEC msdb.dbo.rds_backup_database " +
                                "@source_db_name='%s', " +
                                "@overwrite_s3_backup_file=1," +
                                "@s3_arn_to_backup_to='%s', " +
                                "@type='FULL';",
                        bean.getSourceDatabase(), s3Arn);

                try (ResultSet rs = st.executeQuery(backupQuery)) {
                    if (rs.next()) {
                        bean.setRestoreKey(rs.getString("task_id"));
                    }
                    else {
                        throw new SQLException("Erro ao iniciar o backup");
                    }
                }

                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup iniciado com sucesso!", Level.INFO, this.getClass(), true));
                while (true) {
                    if (dto.isCanceled()) {
                        String cancelQuery = String.format("EXEC msdb.dbo.rds_cancel_task @task_id='%s';", bean.getRestoreKey());
                        try (ResultSet rs = st.executeQuery(cancelQuery)) {
                            if (rs.next()) {
                                throw new InterruptedException("Backup cancelado pelo usuário");
                            }
                        }
                        break;
                    }
                    boolean status = checkStatus(connection, bean.getRestoreKey(), dto);
                    if (status) {
                        break;
                    }
                    Thread.sleep(5000);
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup finalizado com sucesso!", Level.INFO, this.getClass(), true));

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


    @Override
    public Future<BackupRestoreRDSDTO> engineRestore(BackupRestoreRDSDTO dto) {
        ActionRDSBean bean = dto.getBean();
        RDSDTO rds = bean.getRds();

        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }

        String s3Arn = bean.getDumpFile().getArn();

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;

            try (Connection connection = this.getConnection(rds);
                 Statement st = connection.createStatement()) {

                String checkDatabaseQuery = String.format(
                        "SELECT name FROM sys.databases WHERE UPPER(name)='%s';",
                        bean.getDestinationDatabase());

                // Verifica se o banco já existe e exclui, se necessário
                boolean exists = false;
                try (ResultSet rs = st.executeQuery(checkDatabaseQuery)) {
                    exists = rs.next();
                }
                if (exists) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados já existe. Removendo: " + bean.getDestinationDatabase(), Level.DEBUG, this.getClass(), true));
                    String dropQuery = String.format("DROP DATABASE IF EXISTS %s;", bean.getDestinationDatabase());
                    st.execute(dropQuery);
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Banco de dados removido com sucesso.", Level.DEBUG, this.getClass(), true));
                }
                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                String restoreQuery = String.format(
                        "EXEC msdb.dbo.rds_restore_database " +
                                "@restore_db_name='%s', " +
                                "@s3_arn_to_restore_from='%s';",
                        bean.getDestinationDatabase(), s3Arn);

                try (ResultSet rs = st.executeQuery(restoreQuery)) {
                    if (rs.next()) {
                        bean.setRestoreKey(rs.getString("task_id"));
                    }
                    else {
                        throw new SQLException("Erro ao iniciar o backup");
                    }
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restore iniciado com sucesso!", Level.INFO, this.getClass(), true));

                while (true) {
                    if (dto.isCanceled()) {
                        String cancelQuery = String.format("EXEC msdb.dbo.rds_cancel_task @task_id='%s';", bean.getRestoreKey());
                        try (ResultSet rs = st.executeQuery(cancelQuery)) {
                            if (rs.next()) {
                                throw new InterruptedException("Restore cancelado pelo usuário");
                            }
                        }
                        break;
                    }
                    boolean status = checkStatus(connection, bean.getRestoreKey(), dto);
                    if (status) {
                        break;
                    }
                    Thread.sleep(5000);
                }

//                dto.addStatus(new BackupRestoreRDSStatusDTO("Revogando restrições de índices...", Level.DEBUG, this.getClass(), false));
//                String revokeSql = "REVOKE SELECT ON OBJECT::sys.sysindexes FROM PUBLIC;";
//
//                try (Statement statement = connection.createStatement()) {
//
//                    statement.execute(revokeSql);
//                    dto.addStatus(new BackupRestoreRDSStatusDTO("Permissão REVOKE executada com sucesso.", Level.DEBUG, this.getClass(), false));
//
//                } catch (Exception e) {
//                    dto.addStatus(new BackupRestoreRDSStatusDTO("Erro ao revogar restrições de índices: " + e.getMessage(), Level.ERROR, this.getClass(), true));
//                    e.printStackTrace();
//                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restore finalizado com sucesso!", Level.INFO, this.getClass(), true));

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

    private boolean checkStatus(Connection connection, String taskId, BackupRestoreRDSDTO dto) throws Exception {
        try (Statement st = connection.createStatement()) {

            String statusQuery = String.format("exec msdb.dbo.rds_task_status @task_id='%s';", taskId);
            try (ResultSet rs = st.executeQuery(statusQuery)) {

                if (rs.next()) {
                    String lifecycle = rs.getString("lifecycle");
                    String taskInfo = rs.getString("task_info");
                    int percentComplete = rs.getInt("% complete");

                    // Verificando o ciclo de vida (status) da tarefa
                    if ("SUCCESS".equalsIgnoreCase(lifecycle)) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Tarefa concluída com sucesso!", Level.DEBUG, this.getClass(), true));
                        return true;
                    } else if ("ERROR".equalsIgnoreCase(lifecycle)) {
                        throw new Exception("Tarefa falhou. Detalhes: " + taskInfo);
                    } else {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Progresso: " + percentComplete + "% - A tarefa ainda está em andamento.", Level.DEBUG, this.getClass(), true));
                        return false; // A tarefa ainda está em andamento
                    }
                }
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao verificar status da tarefa: " + e.getMessage());
        }
        return false; // Caso não tenha encontrado o status ou erro
    }



    private Connection getConnection(RDSDTO rds) throws SQLException {
        String url = String.format("jdbc:sqlserver://%s;encrypt=true;trustServerCertificate=true;", rds.getEndpoint());
        return DriverManager.getConnection(url, rds.getUsername(), rds.getPassword());
    }
}
