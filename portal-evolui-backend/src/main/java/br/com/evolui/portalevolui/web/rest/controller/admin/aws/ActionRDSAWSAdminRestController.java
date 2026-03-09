package br.com.evolui.portalevolui.web.rest.controller.admin.aws;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteFileMapConfigDTO;
import br.com.evolui.portalevolui.web.beans.enums.*;
import br.com.evolui.portalevolui.web.repository.dto.ActionRDSFilterDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSStatusDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.BucketDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import br.com.evolui.portalevolui.web.service.AWSActionService;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin/aws/action-rds")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
public class ActionRDSAWSAdminRestController {

    @Autowired
    private AWSActionService service;

    @GetMapping()
    public ResponseEntity<List<ActionRDSBean>> get() {
        return ResponseEntity.ok(this.service.getRepository().findAll());
    }

    @PostMapping("/filter")
    public ResponseEntity<List<ActionRDSBean>> get(@RequestBody ActionRDSFilterDTO body) throws Exception{
        this.deleteOldResults();
        return ResponseEntity.ok(this.service.getRepository().filter(body));

    }

    @GetMapping("databases")
    public ResponseEntity<LinkedHashMap<String, List<RDSDTO>>> getDatabases() {
        LinkedHashMap<String, List<RDSDTO>> databases = this.service.getDatabases();
        for (List<RDSDTO> list : databases.values()) {
            for (RDSDTO dto : list) {
                dto.setBusy(this.service.isRDSBusy(dto));
            }
        }
        return ResponseEntity.ok(databases);
    }

    @PostMapping("retrieve-schemas")
    public ResponseEntity<LinkedHashSet<String>> retrieveSchemas(@RequestBody RDSDTO body) throws Exception {
        return ResponseEntity.ok(this.service.retrieveRDSSchemas(body));
    }

    @PostMapping("retrieve-tablespaces")
    public ResponseEntity<LinkedHashSet<String>> retrieveTablespaces(@RequestBody RDSDTO body) throws Exception {
        return ResponseEntity.ok(this.service.retrieveRDSTablespaces(body));
    }

    @PostMapping("retrieve-buckets")
    public ResponseEntity<List<BucketDTO>> retrieveBuckets(@RequestBody BucketDTO body) throws Exception {
        return ResponseEntity.ok(this.service.retrieveBuckets(body));
    }

    @PostMapping()
    public ResponseEntity<ActionRDSBean> save(@RequestBody ActionRDSBean body) throws Exception{
        this.validateBean(body);
        ActionRDSBean bean = this.service.backupRestoreRDS(body);
        this.service.createLog(body.getActionType() == ActionRDSTypeEnum.BACKUP ? AWSActionTypeEnum.BACKUP_RDS : AWSActionTypeEnum.RESTORE_RDS, bean.getRds());
        return ResponseEntity.ok(bean);
    }

    @GetMapping("/errors/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getErrors(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id).get();
        LinkedHashMap<String, Object> resp = new LinkedHashMap();
        resp.put("resp", bean.getError());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id).get();
        if (bean.getStatus() != GithubActionStatusEnum.completed && bean.getStatus() != GithubActionStatusEnum.scheduled) {
            throw new Exception("Apenas requisições completas ou agendadas podem ser apagadas");
        }
        if (bean.getStatus() == GithubActionStatusEnum.scheduled) {
            AWSActionService.cancelSchedule(bean);
        }

        this.service.getRepository().deleteById(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActionRDSBean> get(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id).get();
        return ResponseEntity.ok(bean);
    }

    @PutMapping("/rerun-failed/{id}")
    public ResponseEntity<ActionRDSBean> rerun(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id).get();
        if (bean.getStatus() != GithubActionStatusEnum.completed) {
            throw new Exception("A requisição ainda não foi processada. Espere o término e tente novamente");
        }
        if (bean.getConclusion() == GithubActionConclusionEnum.success) {
            throw new Exception("A requisição foi finalizada com sucesso. Não pode ser atualizada");
        }
        this.validateBean(bean);
        ActionRDSBean saved = this.service.backupRestoreRDS(bean);
        this.service.createLog(saved.getActionType() == ActionRDSTypeEnum.BACKUP ? AWSActionTypeEnum.BACKUP_RDS : AWSActionTypeEnum.RESTORE_RDS, bean.getRds());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancel(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id).get();
        if (bean.getStatus() != GithubActionStatusEnum.in_progress && bean.getStatus() != GithubActionStatusEnum.queued) {
            throw new Exception("Apenas requisições em curso podem ser canceladas");
        }
        this.service.cancel(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping(value = "/status/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBackupRestoreStatus(@PathVariable("id") Long id) throws Exception {
        ActionRDSBean bean = this.service.getRepository().findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));

        if (bean.getStatus() != GithubActionStatusEnum.in_progress && bean.getStatus() != GithubActionStatusEnum.queued) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas requisições em curso podem ser monitoradas");
        }

        BackupRestoreRDSDTO dto = this.service.getBackupRestore(id);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não foi possível recuperar o status da requisição");
        }

        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                int heartbeatCounter = 0;
                dto.registerListener();
                while (!dto.isCanceled()) {
                    BackupRestoreRDSStatusDTO status = dto.readLog();

                    if (status != null) {
                        emitter.send(status, MediaType.APPLICATION_JSON);

                        if (status.isFinished()) {
                            emitter.complete();
                            break;
                        }
                        heartbeatCounter = 0;
                    } else {
                        // Se não há novos logs, envie um heartbeat a cada X iterações
                        if (heartbeatCounter++ % 10 == 0) { // a cada 5 segundos (10 * 500ms)
                            try {
                                BackupRestoreRDSStatusDTO heartbeatStatus = new BackupRestoreRDSStatusDTO(
                                        "heartbeat",
                                        Level.DEBUG,
                                        ActionRDSAWSAdminRestController.class,
                                        false
                                );
                                // Adicionar um campo para identificar como heartbeat
                                heartbeatStatus.setHeartbeat(true);

                                emitter.send(heartbeatStatus, MediaType.APPLICATION_JSON);
                                heartbeatCounter = 1;
                            } catch (Exception e) {
                                // Se falhar ao enviar, o cliente provavelmente desconectou
                                System.out.println("Cliente desconectado: " + e.getMessage());
                                emitter.complete();
                                break;
                            }
                        }
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                dto.unregisterListener();
            }
        });

        emitter.onCompletion(() -> {
            dto.unregisterListener();
            shutdownExecutor(executor);
        });

        emitter.onTimeout(() -> {
            dto.unregisterListener();
            shutdownExecutor(executor);
        });

        emitter.onError((e) -> {
            dto.unregisterListener();
            shutdownExecutor(executor);
            emitter.complete();
        });
        return emitter;
    }

    private void deleteOldResults() {
        if (AWSActionService.getDeleteSemaphore().getQueueLength() == 0) {
            this.service.deleteOldResults();
        }
    }

    private void validateBean(ActionRDSBean bean) throws Exception {
        if (this.service.isRDSBusy(bean.getRds())) {
            throw new Exception("O RDS selecionado está ocupado");
        }
        if (bean.getActionType() == null) {
            throw new Exception("Ação não informada");
        }
        if (bean.getActionType() == ActionRDSTypeEnum.BACKUP) {
            LinkedHashSet<String> schemas = this.service.retrieveRDSSchemas(bean.getRds());
            if (!schemas.contains(bean.getSourceDatabase())) {
                throw new Exception("O schema " + bean.getSourceDatabase() + " não existe no RDS selecionado");
            }
        }
        else if (bean.getActionType() == ActionRDSTypeEnum.RESTORE) {
            if (!this.service.getAWSService().bucketFileExists(bean.getDumpFile())) {
                throw new Exception("O arquivo " + bean.getDumpFile() + " não existe no bucket selecionado");
            }
            if (bean.getRemaps() != null && 
                bean.getRemaps().containsKey(ActionRDSRemapTypeEnum.TABLESPACE) && 
                bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE) != null && 
                !bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE).isEmpty()) {
                
                LinkedHashSet<String> tablespaces = this.service.retrieveRDSTablespaces(bean.getRds());
                List<AmbienteFileMapConfigDTO> tablespaceList = bean.getRemaps().get(ActionRDSRemapTypeEnum.TABLESPACE);

                if (tablespaceList != null) {
                    Set<String> sources = new HashSet<>();
                    for (AmbienteFileMapConfigDTO tablespaceMap : tablespaceList) {
                        if (StringUtils.hasText(tablespaceMap.getSource())) {
                            String source = tablespaceMap.getSource().trim();
                            if (sources.contains(source)) {
                                throw new Exception("Não é permitido mapear o mesmo tablespace de origem mais de uma vez. Tablespace duplicado: " + source);
                            }
                            if (tablespaceMap != null && StringUtils.hasText(tablespaceMap.getDestination())) {
                                if (!tablespaces.contains(tablespaceMap.getDestination())) {
                                    throw new Exception("O tablespace " + tablespaceMap.getDestination() + " não existe no RDS selecionado");
                                }
                            }
                        }
                    }
                }
            }
        }

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
