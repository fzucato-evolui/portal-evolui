package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteFileMapConfigDTO;
import br.com.evolui.portalevolui.web.beans.enums.ActionRDSRemapTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ActionRDSOracleHelperService extends ActionRDSHelperService {

    @Override
    public LinkedHashSet<String> retrieveRDSSchemas(RDSDTO rds) throws SQLException {
        LinkedHashSet<String> schemas = new LinkedHashSet<>();
        try (Connection connection = this.getConnection(rds);
             Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT username FROM all_users ORDER BY username ASC")) {
                while (rs.next()) {
                    schemas.add(rs.getString("username"));
                }
            }

            return schemas;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public LinkedHashSet<String> retrieveRDSTablespaces(RDSDTO rds) throws SQLException {
        LinkedHashSet<String> schemas = new LinkedHashSet<>();
        try (Connection connection = this.getConnection(rds);
             Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT " +
                    "tablespace_name, status, contents, logging, extent_management, allocation_type\n" +
                    "FROM dba_tablespaces WHERE\n" +
                    "allocation_type = 'SYSTEM' AND\n" +
                    "contents = 'PERMANENT' AND\n" +
                    "status = 'ONLINE' AND\n" +
                    "tablespace_name NOT IN ('SYSTEM', 'SYSAUX') ORDER BY tablespace_name ASC")) {
                while (rs.next()) {
                    schemas.add(rs.getString("tablespace_name"));
                }
            }

            return schemas;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Future<BackupRestoreRDSDTO> engineBackup(BackupRestoreRDSDTO dto) {
        ActionRDSBean bean = dto.getBean();
        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }
        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            String taskKey = null;
            RDSDTO rds = bean.getRds();
            String fileName = bean.getDumpFile().getName();
            String dirName = "DATA_PUMP_DIR"; // Diretório padrão
            if (bean.getRemaps() != null &&
                    bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.DUMP_DIR) &&
                    bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR) != null &&
                    !bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR).isEmpty()) {
                
                List<AmbienteFileMapConfigDTO> dirList = bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR);
                if (dirList != null && !dirList.isEmpty() && 
                    dirList.get(0) != null && 
                    StringUtils.hasText(dirList.get(0).getDestination())) {
                    dirName = dirList.get(0).getDestination();
                }
            }
            String directoryName = dirName;
            String fileLogName = fileName.replace(".dmp", ".log");
            String schemaName = bean.getSourceDatabase();
            String jobName = "EXPORT_" + schemaName + "_" + UUID.randomUUID();
            String fileRegex = String.format("^%s\\..*(log|dmp)$", fileName.replace(".dmp", ""));
            String bucketName = bean.getDumpFile().getBucket();
            String s3Prefix = bean.getDumpFile().getPrefix();
            try (Connection connection = this.getConnection(rds);
                 Statement st = connection.createStatement()) {

                this.removeFiles(dto, directoryName, fileRegex, rds, connection);
                if (dto.isCanceled()) {
                    throw new InterruptedException("Backup cancelado pelo usuário");
                }
                String backupQuery = String.format(
                        "DECLARE\n" +
                                "    hdnl NUMBER;\n" +
                                "BEGIN\n" +
                                "    hdnl := DBMS_DATAPUMP.OPEN( operation => 'EXPORT', job_mode => 'SCHEMA', job_name=> '%s');\n" +
                                "    DBMS_DATAPUMP.ADD_FILE( handle => hdnl, filename => '%s', directory => '%s', filetype => dbms_datapump.ku$_file_type_dump_file);\n" +
                                "    DBMS_DATAPUMP.ADD_FILE( handle => hdnl, filename => '%s', directory => '%s', filetype => dbms_datapump.ku$_file_type_log_file);\n" +
                                "    DBMS_DATAPUMP.METADATA_FILTER(hdnl,'SCHEMA_EXPR','IN (''%s'')');\n" +
                                "    DBMS_DATAPUMP.START_JOB(hdnl);\n" +
                                "END;\n",
                        jobName, fileName, directoryName, fileLogName, directoryName, schemaName.toUpperCase());
                dto.addStatus(new BackupRestoreRDSStatusDTO(String.format("Query de Backup: %s",backupQuery), Level.DEBUG, this.getClass(), true));
                st.execute(backupQuery);
                
                if (dto.isCanceled()) {
                    throw new InterruptedException("Backup cancelado pelo usuário");
                }
                
                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup iniciado com sucesso!", Level.INFO, this.getClass(), true));

                Thread.sleep(5000); // Atraso para criar o job
                String logQuery = String.format(
                        "SELECT TEXT FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('%s', '%s'))",
                        directoryName, fileLogName);

                dto.addStatus(new BackupRestoreRDSStatusDTO("Executando consulta de log: " + logQuery, Level.DEBUG, this.getClass(), true));
                {
                    Pattern fatalErrorPattern = Pattern.compile(String.format(".*%s.*(erro|error|fail|falha|falhou).*", jobName), Pattern.CASE_INSENSITIVE);
                    Pattern successPattern = Pattern.compile(String.format(".*%s.*(success|sucesso|completed).*", jobName), Pattern.CASE_INSENSITIVE);
                    Pattern errorPattern = Pattern.compile(".*(error|fail|exception|erro|falha|falhou|critical).*", Pattern.CASE_INSENSITIVE);
                    List<String> logs = new ArrayList<>();
                    boolean processCompleted = false;
                    while (true) {
                        if (dto.isCanceled()) {
                            String cancelQuery = String.format("BEGIN\n" +
                                    "    DBMS_DATAPUMP.ATTACH('%s');\n" +
                                    "    DBMS_DATAPUMP.KILL_JOB;\n" +
                                    "END;", jobName);
                            st.execute(cancelQuery);
                            throw new RuntimeException("Operação cancelada pelo usuário");
                        }
                        if (checkDataPumpJobStatus(connection, jobName) && processCompleted) {
                            break;
                        }
                        try (ResultSet rs = st.executeQuery(logQuery)) {
                            while (rs.next()) {
                                String line = rs.getString("TEXT");
                                if (logs.contains(line)) {
                                    continue;
                                }
                                logs.add(line);
                                dto.addStatus(new BackupRestoreRDSStatusDTO(line, Level.DEBUG, this.getClass(), false));
                                // Verifica se a linha contém erro fatal, usando regex
                                Matcher fatalErrorMatcher = fatalErrorPattern.matcher(line);
                                if (fatalErrorMatcher.find()) {
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> fatalErrorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    throw new RuntimeException("Erro fatal detectado: \n" + errors);
                                }

                                // Verifica se a linha contém sucesso, usando regex
                                Matcher successMatcher = successPattern.matcher(line);
                                if (successMatcher.find()) {
                                    processCompleted = true;
                                    break;
                                }

                                // Verifica se a linha contém erro genérico, usando regex
                                Matcher errorMatcher = errorPattern.matcher(line);
                                if (errorMatcher.find()) {
                                    // Captura todos os erros e adiciona à lista de erros
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> errorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    dto.addStatus(new BackupRestoreRDSStatusDTO("Erro encontrado: " + errors, Level.WARN, this.getClass(), true));
                                }


                            }
                        }
                        Thread.sleep(2000);
                    }
                }
                Thread.sleep(5000); // Atraso para finalizar o job
                String checkDumpQuery = String.format(
                        "SELECT * FROM TABLE(RDSADMIN.RDS_FILE_UTIL.LISTDIR('%s')) WHERE FILENAME='%s'",
                        directoryName, fileName);
                try (ResultSet rs = st.executeQuery(checkDumpQuery)) {
                    if (!rs.next()) {
                        throw new RuntimeException("Arquivo de dump não encontrado no diretório");
                    }
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Backup finalizado com sucesso!", Level.INFO, this.getClass(), true));
                dto.addStatus(new BackupRestoreRDSStatusDTO("Fazendo o upload para o bucket...", Level.INFO, this.getClass(), true));
                
                String query = String.format("SELECT RDSADMIN.RDSADMIN_S3_TASKS.UPLOAD_TO_S3(\n" +
                        "      p_bucket_name    =>  '%s',\n" +
                        "      p_prefix         =>  '%s', \n" +
                        "      p_s3_prefix      =>  '%s', \n" +
                        "      p_directory_name =>  '%s') \n" +
                        "   AS TASK_ID FROM DUAL", bucketName, fileName.replace(".dmp", ""), s3Prefix, directoryName);
                try (ResultSet rs = st.executeQuery(query)) {
                    if (rs.next()) {
                        taskKey = rs.getString("TASK_ID");
                    } else {
                        throw new SQLException("Erro ao realizar upload do arquivo de dump");
                    }
                }
                Thread.sleep(5000); // Atraso para criar o job
                
                logQuery = String.format(
                        "SELECT TEXT FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('%s', 'dbtask-%s.log'))",
                        "BDUMP", taskKey);
                {
                    Pattern fatalErrorPattern = Pattern.compile("task failed", Pattern.CASE_INSENSITIVE);
                    Pattern successPattern = Pattern.compile("task finished successfully", Pattern.CASE_INSENSITIVE);
                    Pattern errorPattern = Pattern.compile(".*(error|fail|exception|erro|falha|falhou|critical).*", Pattern.CASE_INSENSITIVE);
                    List<String> logs = new ArrayList<>();
                    boolean processCompleted = false;
                    while (true) {
                        if (dto.isCanceled()) {
                            String cancelQuery = String.format("BEGIN\n" +
                                    "    RDSADMIN.RDS_FILE_UTIL.ABORT_TASK('%s');\n" +
                                    "END;", taskKey);
                            st.execute(cancelQuery);
                            throw new RuntimeException("Operação cancelada pelo usuário");
                        }
                        try (ResultSet rs = st.executeQuery(logQuery)) {
                            while (rs.next()) {
                                String line = rs.getString("TEXT");
                                if (logs.contains(line)) {
                                    continue;
                                }
                                logs.add(line);
                                
                                dto.addStatus(new BackupRestoreRDSStatusDTO(line, Level.DEBUG, this.getClass(), false));

                                // Verifica se a linha contém erro fatal, usando regex
                                Matcher fatalErrorMatcher = fatalErrorPattern.matcher(line);
                                if (fatalErrorMatcher.find()) {
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> fatalErrorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    throw new RuntimeException("Erro fatal detectado: \n" + errors);
                                }

                                // Verifica se a linha contém sucesso, usando regex
                                Matcher successMatcher = successPattern.matcher(line);
                                if (successMatcher.find()) {
                                    processCompleted = true;
                                    break;
                                }

                                // Verifica se a linha contém erro genérico, usando regex
                                Matcher errorMatcher = errorPattern.matcher(line);
                                if (errorMatcher.find()) {
                                    // Captura todos os erros e adiciona à lista de erros
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> errorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    dto.addStatus(new BackupRestoreRDSStatusDTO("Erro encontrado: " + errors, Level.WARN, this.getClass(), true));
                                }
                            }
                        }
                        if (processCompleted) {
                            break;
                        }
                        Thread.sleep(1000);
                    }
                }
                bean.setRestoreKey(jobName);
            } catch (Throwable e) {
                error = e;
            } finally {
                try {
                    this.removeFiles(dto, directoryName, fileRegex, rds, null);
                    if (StringUtils.hasText(taskKey)) {
                        this.removeFiles(dto, "BDUMP", String.format("dbtask-%s.log", taskKey), rds, null);
                    }
                }
                catch (Exception ex) {
                    System.err.println("Erro ao remover arquivos temporários: " + ex.getMessage());
                }
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
        if (!StringUtils.hasText(bean.getDumpFile().getName())) {
            throw new RuntimeException("Arquivo de dump não informado");
        }

        return CompletableFuture.supplyAsync(() -> {
            Throwable error = null;
            String taskKey = null;
            RDSDTO rds = bean.getRds();
            String fileName = bean.getDumpFile().getName();
            String dirName = "DATA_PUMP_DIR"; // Diretório padrão
            if (bean.getRemaps() != null &&
                    bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.DUMP_DIR) &&
                    bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR) != null &&
                    !bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR).isEmpty()) {
                
                List<AmbienteFileMapConfigDTO> dirList = bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR);
                if (dirList != null && !dirList.isEmpty() && 
                    dirList.get(0) != null && 
                    StringUtils.hasText(dirList.get(0).getDestination())) {
                    dirName = dirList.get(0).getDestination();
                }
            }
            String directoryName = dirName;
            String fileLogName = fileName.replace(".dmp", ".log");
            String schemaName = bean.getDestinationDatabase();
            String jobName = "IMPORT_" + schemaName + "_" + UUID.randomUUID();
            String fileRegex = String.format("^%s\\..*(log|dmp)$", fileName.replace(".dmp", ""));
            String bucketName = bean.getDumpFile().getBucket();
            String s3Prefix = bean.getDumpFile().getKey();
            String sourceDatabase = bean.getDestinationDatabase();

            try (Connection connection = this.getConnection(rds);
                 Statement st = connection.createStatement()) {

                this.removeFiles(dto, directoryName, fileRegex, rds, connection);
                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                
                dto.addStatus(new BackupRestoreRDSStatusDTO("Fazendo o download de dump do bucket...", Level.INFO, this.getClass(), true));

                String query = String.format("SELECT RDSADMIN.RDSADMIN_S3_TASKS.DOWNLOAD_FROM_S3(\n" +
                        "      p_bucket_name    =>  '%s',\n" +
                        "      p_s3_prefix      =>  '%s', \n" +
                        "      p_directory_name =>  '%s') \n" +
                        "   AS TASK_ID FROM DUAL", bucketName, s3Prefix, directoryName);
                try (ResultSet rs = st.executeQuery(query)) {
                    if (rs.next()) {
                        taskKey = rs.getString("TASK_ID");
                    } else {
                        throw new SQLException("Erro ao realizar download do arquivo de dump");
                    }
                }
                Thread.sleep(5000); // Atraso para criar o job
                String logQuery = String.format(
                        "SELECT TEXT FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('%s', 'dbtask-%s.log'))",
                        "BDUMP", taskKey);
                {
                    Pattern fatalErrorPattern = Pattern.compile("task failed", Pattern.CASE_INSENSITIVE);
                    Pattern successPattern = Pattern.compile("task finished successfully", Pattern.CASE_INSENSITIVE);
                    Pattern errorPattern = Pattern.compile(".*(error|fail|exception|erro|falha|falhou|critical).*", Pattern.CASE_INSENSITIVE);
                    List<String> logs = new ArrayList<>();
                    boolean processCompleted = false;
                    
                    while (true) {
                        if (dto.isCanceled()) {
                            String cancelQuery = String.format("BEGIN\n" +
                                    "    RDSADMIN.RDS_FILE_UTIL.ABORT_TASK('%s');\n" +
                                    "END;", taskKey);
                            st.execute(cancelQuery);
                            throw new RuntimeException("Operação cancelada pelo usuário");
                        }
                        
                        try (ResultSet rs = st.executeQuery(logQuery)) {
                            while (rs.next()) {
                                String line = rs.getString("TEXT");
                                if (logs.contains(line)) {
                                    continue;
                                }
                                logs.add(line);
                                dto.addStatus(new BackupRestoreRDSStatusDTO(line, Level.DEBUG, this.getClass(), false));

                                // Verifica se a linha contém erro fatal, usando regex
                                Matcher fatalErrorMatcher = fatalErrorPattern.matcher(line);
                                if (fatalErrorMatcher.find()) {
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> fatalErrorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    throw new RuntimeException("Erro fatal detectado: \n" + errors);
                                }

                                // Verifica se a linha contém sucesso, usando regex
                                Matcher successMatcher = successPattern.matcher(line);
                                if (successMatcher.find()) {
                                    processCompleted = true;
                                    break;
                                }

                                // Verifica se a linha contém erro genérico, usando regex
                                Matcher errorMatcher = errorPattern.matcher(line);
                                if (errorMatcher.find()) {
                                    // Captura todos os erros e adiciona à lista de erros
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> errorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    dto.addStatus(new BackupRestoreRDSStatusDTO("Erro encontrado: " + errors, Level.WARN, this.getClass(), true));
                                }
                            }
                        }
                        if (processCompleted) {
                            break;
                        }
                        Thread.sleep(1000);
                        
                    }
                }

                String checkUserQuery = String.format(
                        "SELECT username FROM all_users WHERE UPPER(username) = '%s'",
                        bean.getDestinationDatabase().toUpperCase());

                boolean exists = false;
                try (ResultSet rs = st.executeQuery(checkUserQuery)) {
                    exists = rs.next();
                }

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }

                if (exists) {
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Schema já existe. Removendo: " + bean.getDestinationDatabase(), Level.INFO, this.getClass(), true));
                    String dropUserQuery = String.format("DROP USER %s CASCADE", bean.getDestinationDatabase());
                    st.execute(dropUserQuery);
                    dto.addStatus(new BackupRestoreRDSStatusDTO("Schema removido com sucesso.", Level.INFO, this.getClass(), true));
                }

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }

                dto.addStatus(new BackupRestoreRDSStatusDTO("Criando novo Schema/User e dando suas permissões: " + bean.getDestinationDatabase(), Level.INFO, this.getClass(), true));

                String createUserQuery = String.format(
                        "CREATE USER %s IDENTIFIED BY %s DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP",
                        bean.getDestinationDatabase(), bean.getDestinationPassword());
                st.execute(createUserQuery);

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                
                String grantPrivilegesQuery = String.format(
                        "GRANT CONNECT, DBA, EXP_FULL_DATABASE, IMP_FULL_DATABASE, RESOURCE TO %s WITH ADMIN OPTION",
                        bean.getDestinationDatabase());
                st.execute(grantPrivilegesQuery);

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                
                String changeUserRoleQuery = String.format(
                        "ALTER USER %s DEFAULT ROLE CONNECT, DBA, EXP_FULL_DATABASE, IMP_FULL_DATABASE, RESOURCE",
                        bean.getDestinationDatabase());
                st.execute(changeUserRoleQuery);

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                
                // Conceder QUOTA UNLIMITED em todos os tablespaces mapeados
                if (bean.getRemaps() != null &&
                        bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.TABLESPACE) &&
                        bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE) != null) {
                    
                    List<AmbienteFileMapConfigDTO> tablespaceList = bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE);
                    if (tablespaceList != null && !tablespaceList.isEmpty()) {
                        // Primeiro, concede QUOTA no tablespace padrão USERS se necessário
                        boolean hasDefaultTablespace = false;
                        for (AmbienteFileMapConfigDTO tablespaceMap : tablespaceList) {
                            if (tablespaceMap != null && "USERS".equalsIgnoreCase(tablespaceMap.getDestination())) {
                                hasDefaultTablespace = true;
                                break;
                            }
                        }
                        
                        if (!hasDefaultTablespace) {
                            String defaultQuotaQuery = String.format(
                                    "ALTER USER %s QUOTA UNLIMITED ON USERS",
                                    bean.getDestinationDatabase());
                            st.execute(defaultQuotaQuery);
                            
                            if (dto.isCanceled()) {
                                throw new InterruptedException("Restore cancelado pelo usuário");
                            }
                        }
                        
                        // Concede QUOTA em todos os tablespaces mapeados
                        for (AmbienteFileMapConfigDTO tablespaceMap : tablespaceList) {
                            if (tablespaceMap != null && StringUtils.hasText(tablespaceMap.getDestination())) {
                                String quotaQuery = String.format(
                                        "ALTER USER %s QUOTA UNLIMITED ON %s",
                                        bean.getDestinationDatabase(), tablespaceMap.getDestination());
                                st.execute(quotaQuery);
                                
                                if (dto.isCanceled()) {
                                    throw new InterruptedException("Restore cancelado pelo usuário");
                                }
                            }
                        }
                    } else {
                        // Se não houver tablespaces mapeados, concede apenas no USERS padrão
                        String defaultQuotaQuery = String.format(
                                "ALTER USER %s QUOTA UNLIMITED ON USERS",
                                bean.getDestinationDatabase());
                        st.execute(defaultQuotaQuery);
                    }
                } else {
                    // Se não houver mapeamentos de tablespace, concede apenas no USERS padrão
                    String defaultQuotaQuery = String.format(
                            "ALTER USER %s QUOTA UNLIMITED ON USERS",
                            bean.getDestinationDatabase());
                    st.execute(defaultQuotaQuery);
                }

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }
                
                grantPrivilegesQuery = String.format("GRANT ADVISOR, ADMINISTER SQL TUNING SET, ADMINISTER ANY SQL TUNING SET, CREATE ANY SQL PROFILE, ALTER ANY SQL PROFILE, DROP ANY SQL PROFILE, ADMINISTER SQL MANAGEMENT OBJECT, CREATE CLUSTER, CREATE ANY CLUSTER, ALTER ANY CLUSTER, DROP ANY CLUSTER, CREATE ANY CONTEXT, DROP ANY CONTEXT, ALTER ANY ASSEMBLY, CREATE ANY ASSEMBLY, CREATE ASSEMBLY, DROP ANY ASSEMBLY, EXECUTE ANY ASSEMBLY, EXECUTE ASSEMBLY, AUDIT SYSTEM, CREATE DATABASE LINK, CREATE PUBLIC DATABASE LINK, ALTER DATABASE LINK, ALTER PUBLIC DATABASE LINK, DROP PUBLIC DATABASE LINK, DEBUG CONNECT SESSION, DEBUG ANY PROCEDURE, ANALYZE ANY DICTIONARY, CREATE DIMENSION, CREATE ANY DIMENSION, ALTER ANY DIMENSION, DROP ANY DIMENSION, DROP ANY DIRECTORY, ALTER ANY EDITION, CREATE ANY EDITION, DROP ANY EDITION, EXPORT FULL DATABASE, IMPORT FULL DATABASE, FLASHBACK ARCHIVE ADMINISTER, CREATE ANY INDEX, ALTER ANY INDEX, DROP ANY INDEX, CREATE INDEXTYPE, CREATE ANY INDEXTYPE, ALTER ANY INDEXTYPE, DROP ANY INDEXTYPE, EXECUTE ANY INDEXTYPE, CREATE JOB, CREATE ANY JOB, EXECUTE ANY PROGRAM, EXECUTE ANY CLASS, CREATE LIBRARY, CREATE ANY LIBRARY, ALTER ANY LIBRARY, DROP ANY LIBRARY, EXECUTE ANY LIBRARY, CREATE MATERIALIZED VIEW, CREATE ANY MATERIALIZED VIEW, ALTER ANY MATERIALIZED VIEW, DROP ANY MATERIALIZED VIEW, QUERY REWRITE, GLOBAL QUERY REWRITE, ON COMMIT REFRESH, FLASHBACK ANY TABLE, CREATE MINING MODEL, CREATE ANY MINING MODEL, ALTER ANY MINING MODEL, DROP ANY MINING MODEL, SELECT ANY MINING MODEL, COMMENT ANY MINING MODEL, CREATE CUBE, CREATE ANY CUBE, ALTER ANY CUBE, DROP ANY CUBE, SELECT ANY CUBE, UPDATE ANY CUBE, CREATE MEASURE FOLDER, CREATE ANY MEASURE FOLDER, DELETE ANY MEASURE FOLDER, DROP ANY MEASURE FOLDER, INSERT ANY MEASURE FOLDER, CREATE CUBE DIMENSION, CREATE ANY CUBE DIMENSION, ALTER ANY CUBE DIMENSION, DELETE ANY CUBE DIMENSION, DROP ANY CUBE DIMENSION, INSERT ANY CUBE DIMENSION, SELECT ANY CUBE DIMENSION, UPDATE ANY CUBE DIMENSION, CREATE CUBE BUILD PROCESS, CREATE ANY CUBE BUILD PROCESS, DROP ANY CUBE BUILD PROCESS, UPDATE ANY CUBE BUILD PROCESS, CREATE OPERATOR, CREATE ANY OPERATOR, ALTER ANY OPERATOR, DROP ANY OPERATOR, EXECUTE ANY OPERATOR, CREATE ANY OUTLINE, ALTER ANY OUTLINE, DROP ANY OUTLINE, CREATE PROCEDURE, CREATE ANY PROCEDURE, ALTER ANY PROCEDURE, DROP ANY PROCEDURE, EXECUTE ANY PROCEDURE, CREATE PROFILE, ALTER PROFILE, DROP PROFILE, CREATE ROLE, ALTER ANY ROLE, DROP ANY ROLE, CREATE ROLLBACK SEGMENT, ALTER ROLLBACK SEGMENT, DROP ROLLBACK SEGMENT, CREATE SEQUENCE, CREATE ANY SEQUENCE, ALTER ANY SEQUENCE, DROP ANY SEQUENCE, SELECT ANY SEQUENCE, CREATE SESSION, ALTER RESOURCE COST, ALTER SESSION, RESTRICTED SESSION, CREATE SYNONYM, CREATE ANY SYNONYM, CREATE PUBLIC SYNONYM, DROP ANY SYNONYM, DROP PUBLIC SYNONYM, CREATE TABLE, CREATE ANY TABLE, ALTER ANY TABLE, BACKUP ANY TABLE, DELETE ANY TABLE, DROP ANY TABLE, INSERT ANY TABLE, LOCK ANY TABLE, SELECT ANY TABLE, UPDATE ANY TABLE, UNDER ANY TABLE, CREATE TABLESPACE, ALTER TABLESPACE, DROP TABLESPACE, MANAGE TABLESPACE, UNLIMITED TABLESPACE, CREATE TRIGGER, CREATE ANY TRIGGER, ALTER ANY TRIGGER, DROP ANY TRIGGER, ADMINISTER DATABASE TRIGGER, CREATE TYPE, CREATE ANY TYPE, ALTER ANY TYPE, DROP ANY TYPE, EXECUTE ANY TYPE, UNDER ANY TYPE, CREATE USER, ALTER USER, DROP USER, CREATE VIEW, CREATE ANY VIEW, DROP ANY VIEW, UNDER ANY VIEW, MERGE ANY VIEW, ANALYZE ANY, AUDIT ANY, BECOME USER, CHANGE NOTIFICATION, COMMENT ANY TABLE, EXEMPT ACCESS POLICY, EXEMPT IDENTITY POLICY, FORCE ANY TRANSACTION, FORCE TRANSACTION, GRANT ANY OBJECT PRIVILEGE, RESUMABLE, SELECT ANY DICTIONARY, SELECT ANY TRANSACTION TO %s WITH ADMIN OPTION",
                        bean.getDestinationDatabase());
                st.execute(grantPrivilegesQuery);

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }

                dto.addStatus(new BackupRestoreRDSStatusDTO("Schema/User criado com sucesso: " + bean.getDestinationDatabase(), Level.INFO, this.getClass(), true));

                StringBuilder remapTablespace = new StringBuilder();
                if (bean.getRemaps() != null &&
                        bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.TABLESPACE) &&
                        bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE) != null &&
                        !bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE).isEmpty()) {
                    
                    List<AmbienteFileMapConfigDTO> tablespaceList = bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE);
                    if (tablespaceList != null) {
                        for (AmbienteFileMapConfigDTO tablespaceMap : tablespaceList) {
                            if (tablespaceMap != null && StringUtils.hasText(tablespaceMap.getSource()) && 
                                StringUtils.hasText(tablespaceMap.getDestination())) {
                                remapTablespace.append(String.format("DBMS_DATAPUMP.METADATA_REMAP(hdnl,'REMAP_TABLESPACE','%s','%s');\n",
                                    tablespaceMap.getSource(), tablespaceMap.getDestination()));
                            }
                        }
                    }
                }
                StringBuilder remapSchema = new StringBuilder();
                if (bean.getRemaps() != null &&
                        bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.SCHEMA) &&
                        bean.getRemaps().get(ActionRDSRemapTypeEnum.SCHEMA) != null &&
                        !bean.getRemaps().get(ActionRDSRemapTypeEnum.SCHEMA).isEmpty()) {
                    
                    List<AmbienteFileMapConfigDTO> schemaList = bean.getRemaps().get(ActionRDSRemapTypeEnum.SCHEMA);
                    if (schemaList != null) {
                        // Assumimos que o primeiro mapeamento de esquema define a fonte primária
                        if (!schemaList.isEmpty() && schemaList.get(0) != null && 
                            StringUtils.hasText(schemaList.get(0).getSource())) {
                            sourceDatabase = schemaList.get(0).getSource();
                        }
                        
                        // Adicionamos todos os mapeamentos de esquema
                        for (AmbienteFileMapConfigDTO schemaMap : schemaList) {
                            if (schemaMap != null && StringUtils.hasText(schemaMap.getSource()) && 
                                StringUtils.hasText(schemaMap.getDestination())) {
                                remapSchema.append(String.format("DBMS_DATAPUMP.METADATA_REMAP(hdnl,'REMAP_SCHEMA','%s','%s');\n",
                                    schemaMap.getSource(), schemaMap.getDestination()));
                            }
                        }
                    }
                }
                String restoreQuery = String.format(
                        "DECLARE\n" +
                                "    hdnl NUMBER;\n" +
                                "BEGIN\n" +
                                "    hdnl := DBMS_DATAPUMP.OPEN( operation => 'IMPORT', job_mode => 'SCHEMA', job_name=> '%s');\n" +
                                "    DBMS_DATAPUMP.ADD_FILE( handle => hdnl, filename => '%s', directory => '%s', filetype => dbms_datapump.ku$_file_type_dump_file);\n" +
                                "    DBMS_DATAPUMP.ADD_FILE( handle => hdnl, filename => '%s', directory => '%s', filetype => dbms_datapump.ku$_file_type_log_file);\n" +
                                "    DBMS_DATAPUMP.METADATA_FILTER(hdnl,'SCHEMA_EXPR','IN (''%s'')');\n" +
                                "    %s\n" +
                                "    %s\n" +
                                "    DBMS_DATAPUMP.START_JOB(hdnl);\n" +
                                "END;\n",
                        jobName, fileName, directoryName, fileLogName, directoryName, sourceDatabase.toUpperCase(), 
                        remapSchema.toString(), remapTablespace.toString());

                dto.addStatus(new BackupRestoreRDSStatusDTO("Query de Restore: " + restoreQuery, Level.DEBUG, this.getClass(), true));
                st.execute(restoreQuery);

                if (dto.isCanceled()) {
                    throw new InterruptedException("Restore cancelado pelo usuário");
                }

                dto.addStatus(new BackupRestoreRDSStatusDTO("Restore iniciado com sucesso!", Level.INFO, this.getClass(), true));
                logQuery = String.format(
                        "SELECT TEXT FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('%s', '%s'))",
                        directoryName, fileLogName);
                Thread.sleep(5000); // Atraso para criar o job
                {
                    Pattern fatalErrorPattern = Pattern.compile(
                            String.format(".*(?:O job|Job).*[\"\'].*%s.*[\"\'].*(falhou|abortado|abortou|cancelado|cancelou|falhado|failed|aborted|cancelled|killed).*", jobName),
                            Pattern.CASE_INSENSITIVE
                    );
                    Pattern successPattern = Pattern.compile(
                            String.format(".*(?:O job|Job).*[\"\'].*%s.*[\"\'].*(success|sucesso|completed|conclu[ií]do|concluded).*", jobName),
                            Pattern.CASE_INSENSITIVE
                    );
                    Pattern errorPattern = Pattern.compile(
                            ".*(error|fail|exception|erro|falha|falhou|critical|ORA-\\d+).*",
                            Pattern.CASE_INSENSITIVE
                    );
                    Pattern warningPattern = Pattern.compile(
                            String.format(".*(?:O job|Job).*[\"\'].*%s.*[\"\'].*(?:foi conclu[ií]do|completed)(?:.*com|.*with)\\s*(\\d+)\\s*(?:erro\\(s\\)|error\\(s\\)).*", jobName),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    );
                    List<String> logs = new ArrayList<>();
                    boolean processCompleted = false;
                    boolean jobCompleted = false;
                    
                    while (true) {
                        if (dto.isCanceled()) {
                            String cancelQuery = String.format("BEGIN\n" +
                                    "    DBMS_DATAPUMP.ATTACH('%s');\n" +
                                    "    DBMS_DATAPUMP.KILL_JOB;\n" +
                                    "END;", jobName);
                            st.execute(cancelQuery);
                            throw new RuntimeException("Operação cancelada pelo usuário");
                        }
                        if (!jobCompleted) {
                            jobCompleted = checkDataPumpJobStatus(connection, jobName);
                        }
                        if (jobCompleted && processCompleted) {
                            break;
                        }
                        try (ResultSet rs = st.executeQuery(logQuery)) {
                            while (rs.next()) {
                                String line = rs.getString("TEXT");
                                if (!StringUtils.hasText(line)) {
                                    continue;
                                }
                                if (logs.contains(line)) {
                                    continue;
                                }
                                logs.add(line);

                                // Verifica se a linha contém erro fatal, usando regex
                                Matcher fatalErrorMatcher = fatalErrorPattern.matcher(line);
                                if (fatalErrorMatcher.find()) {
                                    String errors = String.join("\n",
                                            logs.stream().filter(l -> fatalErrorPattern.matcher(l).find())
                                                    .collect(Collectors.toList()));
                                    throw new RuntimeException("Erro fatal detectado: \n" + errors);
                                }

                                // Verifica se a linha contém sucesso, usando regex
                                Matcher successMatcher = successPattern.matcher(line);
                                if (successMatcher.find()) {
                                    // Verifica se a conclusão tem warnings (erros não críticos)
                                    Matcher warningMatcher = warningPattern.matcher(line);
                                    if (warningMatcher.find()) {
                                        // Lista para armazenar os erros e suas descrições
                                        List<String> errorMessages = new ArrayList<>();
                                        boolean captureNextLine = false;

                                        // Objetos não críticos quando mencionados com ORA-39083
                                        Set<String> nonCriticalObjects = new HashSet<>(Arrays.asList(
                                                "role_grant",
                                                "default_role",
                                                "system_grant"
                                                // adicione outros objetos não críticos aqui
                                        ));

                                        // Lista de códigos de erro Oracle que podem ser ignorados incondicionalmente
                                        Set<String> ignorableOraErrors = new HashSet<>(Arrays.asList(
                                                "ORA-31684", // Objeto já existe
                                                "ORA-01924", // Atribuição não concedida
                                                "ORA-01919"  // Atribuição não existe
                                                // ORA-39083 será tratado separadamente
                                        ));

                                        // Mensagens que indicam informações, não erros, em ambos os idiomas
                                        Set<String> informationalPatterns = new HashSet<>(Arrays.asList(
                                                ".*(?:processando o tipo de objeto|processing object type).*",
                                                ".*(?:o código sql com falha é|the failing sql statement was).*",
                                                ".*(?:importou|imported).*\".*\".*",
                                                ".*(?:tabela-mestre|master table).*(?:carregada|loaded).*",
                                                ".*(?:iniciando|starting).*\".*\".*"
                                        ));

                                        // Percorre os logs para capturar erros e suas descrições
                                        for (int i = 0; i < logs.size(); i++) {
                                            String currentLine = logs.get(i);
                                            if (currentLine == null) continue;

                                            // Converter para lowercase para comparações case-insensitive
                                            String lowerLine = currentLine.toLowerCase();

                                            // Verifica se é linha informacional para ignorar
                                            boolean isInformational = informationalPatterns.stream()
                                                    .anyMatch(pattern -> lowerLine.matches(pattern));

                                            if (isInformational) continue;

                                            // Extrai código ORA se presente (case-sensitive para manter formato)
                                            String oraCode = null;
                                            java.util.regex.Pattern oraPattern = java.util.regex.Pattern.compile("(ORA-\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
                                            java.util.regex.Matcher oraMatcher = oraPattern.matcher(currentLine);
                                            if (oraMatcher.find()) {
                                                oraCode = oraMatcher.group(1).toUpperCase(); // Normaliza para maiúsculas
                                            }

                                            // Verifica se é uma linha de erro que não deve ser ignorada
                                            if (oraCode != null) {
                                                // Tratamento especial para ORA-39083 (verificar se é objeto não crítico)
                                                if (oraCode.equals("ORA-39083")) {
                                                    boolean isNonCriticalObject = false;
                                                    // Verifica se a linha contém algum dos objetos não críticos
                                                    for (String obj : nonCriticalObjects) {
                                                        if (lowerLine.contains(obj.toLowerCase())) {
                                                            isNonCriticalObject = true;
                                                            break;
                                                        }
                                                    }

                                                    // Se não for objeto não crítico (ex: se for uma tabela), registra como erro
                                                    if (!isNonCriticalObject) {
                                                        errorMessages.add(currentLine.trim());
                                                        captureNextLine = true;
                                                    }
                                                }
                                                // Para outros códigos ORA, verifica na lista de erros ignoráveis
                                                else if (!ignorableOraErrors.contains(oraCode)) {
                                                    errorMessages.add(currentLine.trim());
                                                    captureNextLine = true;
                                                }
                                            }
                                            // Verifica se é uma linha contendo mensagens de erro conhecidas
                                            else if (oraCode == null && // Não é uma linha com ORA code
                                                    lowerLine.matches(".*(error|fail|exception|erro|falha|falhou|critical).*") &&
                                                    !lowerLine.matches(".*(?:concluído com|completed with).*(?:erro|error).*")) {

                                                errorMessages.add(currentLine.trim());
                                                captureNextLine = true;
                                            }
                                            // Captura a linha após o erro, se for relevante
                                            else if (captureNextLine) {
                                                // Verifica se a linha seguinte parece com uma descrição adicional
                                                // e não é uma nova mensagem de erro ou linha informacional
                                                if (!currentLine.trim().isEmpty() &&
                                                        !lowerLine.matches(".*ora-\\d+.*") &&
                                                        !informationalPatterns.stream().anyMatch(pattern -> lowerLine.matches(pattern)) &&
                                                        !lowerLine.startsWith("grant") &&
                                                        !lowerLine.startsWith("alter") &&
                                                        !lowerLine.startsWith("create")) {

                                                    errorMessages.add("  → " + currentLine.trim());
                                                }
                                                captureNextLine = false;
                                            }
                                        }

                                        // Se encontrou mensagens de erro, formata-as para exibição
                                        if (!errorMessages.isEmpty()) {
                                            String formattedErrors = String.join("\n", errorMessages);
                                            dto.addStatus(new BackupRestoreRDSStatusDTO(
                                                    "Job concluído com alguns erros não críticos:\n" + formattedErrors,
                                                    Level.WARN,
                                                    this.getClass(),
                                                    true
                                            ));
                                            dto.setWarning(formattedErrors);
                                        } else {
                                            dto.addStatus(new BackupRestoreRDSStatusDTO(
                                                    "Job concluído com sucesso",
                                                    Level.INFO,
                                                    this.getClass(),
                                                    true
                                            ));
                                        }
                                    }
                                    else {
                                        dto.addStatus(new BackupRestoreRDSStatusDTO("Job concluído com sucesso", Level.INFO, this.getClass(), true));
                                    }
                                    processCompleted = true;
                                    //System.out.println(String.join("\n", logs));
                                    break;
                                }

                                // Verifica se a linha contém erro genérico, usando regex
                                Matcher errorMatcher = errorPattern.matcher(line);
                                if (errorMatcher.find()) {
                                    dto.addStatus(new BackupRestoreRDSStatusDTO(line, Level.WARN, this.getClass(), true));
                                }
                                else {
                                    dto.addStatus(new BackupRestoreRDSStatusDTO(line, Level.DEBUG, this.getClass(), false));
                                }
                            }
                        }
                        Thread.sleep(2000);
                    }
                }
                dto.addStatus(new BackupRestoreRDSStatusDTO("Restore finalizado com sucesso!", Level.INFO, this.getClass(), true));

            } catch (Throwable e) {
                error = e;
            } finally {
                try {
                    this.removeFiles(dto, directoryName, fileRegex, rds, null);
                    if (StringUtils.hasText(taskKey)) {
                        this.removeFiles(dto, "BDUMP", String.format("dbtask-%s.log", taskKey), rds, null);
                    }
                }
                catch (Exception ex) {
                    System.err.println("Erro ao remover arquivos temporários: " + ex.getMessage());
                }
                if (error != null) {
                    dto.setError(error);
                    return dto;
                }
            }
            return dto;
        });
    }

    private boolean checkDataPumpJobStatus(Connection connection, String jobName) throws SQLException {
        try (Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(String.format("SELECT STATE FROM dba_datapump_jobs WHERE JOB_NAME = '%s'", jobName))) {
            if (rs.next()) {
                String state = rs.getString("state");

                if ("EXECUTING".equalsIgnoreCase(state)) {
                    return false;
                }
                if ("COMPLETING".equalsIgnoreCase(state)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void removeFiles(BackupRestoreRDSDTO dto, String directoryName, String filePattern, RDSDTO rds, Connection existingConnection) {
        boolean shouldCloseConnection = false;
        Connection connection = existingConnection;

        try {
            if (connection == null) {
                connection = this.getConnection(rds);
                shouldCloseConnection = true;
            }

            String querySelectFiles = String.format(
                    "SELECT filename FROM TABLE(rdsadmin.rds_file_util.listdir('%s')) " +
                            "WHERE type = 'file' AND REGEXP_LIKE(filename, '%s')", directoryName, filePattern);

            dto.addStatus(new BackupRestoreRDSStatusDTO("Executando consulta de arquivos: " + querySelectFiles, Level.DEBUG, this.getClass(), true));
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(querySelectFiles)) {

                // Processar todos os resultados do SELECT primeiro
                List<String> filesToRemove = new ArrayList<>();
                while (rs.next()) {
                    String file = rs.getString("filename");
                    filesToRemove.add(file);
                }

                // Agora, executar a remoção dos arquivos
                try (Statement deleteStatement = connection.createStatement()) {
                    for (String file : filesToRemove) {
                        dto.addStatus(new BackupRestoreRDSStatusDTO("Removendo arquivo: " + file, Level.DEBUG, this.getClass(), true));
                        String query = String.format("BEGIN\n" +
                                "    UTL_FILE.FREMOVE('%s', '%s');\n" +
                                "END;", directoryName, file);
                        deleteStatement.execute(query);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (shouldCloseConnection && connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }

    public void readLogFileWithRegex(String directoryName, String logFile, RDSDTO rds, Connection existingConnection, String fatalErrorRegex, String successRegex, String errorRegex) {
        boolean shouldCloseConnection = false;
        Connection connection = existingConnection;

        try {
            if (connection == null) {
                connection = this.getConnection(rds);
                shouldCloseConnection = true;
            }

            String logQuery = String.format(
                    "SELECT TEXT FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('%s', '%s'))",
                    directoryName, logFile);

            System.out.println("Executando consulta de log: " + logQuery);

            List<String> logs = new ArrayList<>();
            boolean processCompleted = false;
            boolean fatalErrorDetected = false;

            // Compilar os padrões regex para erro fatal, sucesso e erro genérico
            Pattern fatalErrorPattern = Pattern.compile(fatalErrorRegex, Pattern.CASE_INSENSITIVE);
            Pattern successPattern = Pattern.compile(successRegex, Pattern.CASE_INSENSITIVE);
            Pattern errorPattern = Pattern.compile(errorRegex, Pattern.CASE_INSENSITIVE);

            try (Statement st = connection.createStatement()) {
                // Lê o log em um loop contínuo
                while (true) {
                    try (ResultSet rs = st.executeQuery(logQuery)) {
                        while (rs.next()) {
                            String line = rs.getString("TEXT");

                            // Evita duplicação de linhas no log
                            if (logs.contains(line)) {
                                continue;
                            }

                            System.out.println(line);
                            logs.add(line);

                            // Verifica se a linha contém erro fatal, usando regex
                            Matcher fatalErrorMatcher = fatalErrorPattern.matcher(line);
                            if (fatalErrorMatcher.find()) {
                                fatalErrorDetected = true;
                                String errors = String.join("\n",
                                        logs.stream().filter(l -> fatalErrorPattern.matcher(l).find())
                                                .collect(Collectors.toList()));
                                throw new RuntimeException("Erro fatal detectado: \n" + errors);
                            }

                            // Verifica se a linha contém sucesso, usando regex
                            Matcher successMatcher = successPattern.matcher(line);
                            if (successMatcher.find()) {
                                processCompleted = true;
                                break;
                            }

                            // Verifica se a linha contém erro genérico, usando regex
                            Matcher errorMatcher = errorPattern.matcher(line);
                            if (errorMatcher.find()) {
                                // Captura todos os erros e adiciona à lista de erros
                                String errors = String.join("\n",
                                        logs.stream().filter(l -> errorPattern.matcher(l).find())
                                                .collect(Collectors.toList()));
                                System.out.println("Erro encontrado: " + errors);
                            }
                        }
                    }

                    // Caso o processo tenha sido completado ou um erro fatal tenha ocorrido, sai do loop
                    if (processCompleted || fatalErrorDetected) {
                        break;
                    }

                    // Espera 500 ms antes de verificar novamente
                    Thread.sleep(500);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (shouldCloseConnection && connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }


    public void uploadDumpFile(ActionRDSBean bean) throws InterruptedException {
        RDSDTO rds = bean.getRds();
        String bucketName = bean.getDumpFile().getBucket();
        String s3Prefix = bean.getDumpFile().getPrefix();
        String fileName = bean.getDumpFile().getName();
        String dirName = "DATA_PUMP_DIR"; // Diretório padrão
        if (bean.getRemaps() != null &&
            bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.DUMP_DIR) &&
            bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR) != null &&
            !bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR).isEmpty()) {
            
            List<AmbienteFileMapConfigDTO> dirList = bean.getRemaps().get(ActionRDSRemapTypeEnum.DUMP_DIR);
            if (dirList != null && !dirList.isEmpty() && 
                dirList.get(0) != null && 
                StringUtils.hasText(dirList.get(0).getDestination())) {
                dirName = dirList.get(0).getDestination();
            }
        }
        String directoryName = dirName;

        String query = String.format("SELECT RDSADMIN.RDSADMIN_S3_TASKS.UPLOAD_TO_S3(\n" +
                "      p_bucket_name    =>  '%s',\n" +
                "      p_prefix         =>  '%s', \n" +
                "      p_s3_prefix      =>  '%s', \n" +
                "      p_directory_name =>  '%s') \n" +
                "   AS TASK_ID FROM DUAL", bucketName, fileName, s3Prefix, directoryName);
        try (Connection connection = this.getConnection(rds);
             Statement st = connection.createStatement()) {
             try (ResultSet rs = st.executeQuery(query)) {
                 if (rs.next()) {
                     bean.setRestoreKey(rs.getString("TASK_ID"));
                 }
                 else {
                     throw new RuntimeException("Erro ao realizar upload do arquivo de dump");
                 }
             }
             String logQuery = String.format("SELECT TEXT " +
                     "FROM TABLE(RDSADMIN.RDS_FILE_UTIL.READ_TEXT_FILE('BDUMP', 'dbtask-%s.log'))",
                     bean.getRestoreKey());
             System.out.println(logQuery);
             List<String> logs = new ArrayList<>();
             try {
                 while(true) {
                     try (ResultSet rs = st.executeQuery(logQuery)) {
                         while (rs.next()) {
                             String line = rs.getString("TEXT");
                             if (logs.contains(line)) {
                                 continue;
                             }
                             System.out.println(line);
                             logs.add(line);
                             if (line.toLowerCase().contains("task failed")) {
                                 String errors = String.join("\n", logs.stream().filter(l -> l.toLowerCase().contains("error") || l.toLowerCase().contains("fail")).collect(Collectors.toList()));
                                 throw new RuntimeException("Erro ao realizar upload do arquivo de dump: \n" + errors);
                             }
                             if (line.toLowerCase().contains("task finished successfully")) {
                                 break;
                             }
                         }
                     }
                     Thread.sleep(500);
                 }
             }
             finally {
                 String deleteTaskQuery = String.format("BEGIN \n" +
                         "\tUTL_FILE.FREMOVE('BDUMP', 'dbtask-%s.log');\n" +
                         "END;", bean.getRestoreKey());
                 st.execute(deleteTaskQuery);
             }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection(RDSDTO rds) throws SQLException {
        String url = String.format("jdbc:oracle:thin:@//%s/%s", rds.getEndpoint(), rds.getDbName());
        return DriverManager.getConnection(url, rds.getUsername(), rds.getPassword());
    }
}
