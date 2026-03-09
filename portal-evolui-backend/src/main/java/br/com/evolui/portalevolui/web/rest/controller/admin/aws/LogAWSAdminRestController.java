package br.com.evolui.portalevolui.web.rest.controller.admin.aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.repository.dto.LogAWSActionFilterDTO;
import br.com.evolui.portalevolui.web.service.AWSActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/aws/log")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class LogAWSAdminRestController {

    @Autowired
    private AWSActionService service;

    @GetMapping()
    public ResponseEntity<List<LogAWSActionBean>> get() {
        return ResponseEntity.ok(this.service.getLogRepository().findAll());
    }

    @PostMapping("/filter")
    public ResponseEntity<List<LogAWSActionBean>> get(@RequestBody LogAWSActionFilterDTO body) throws Exception{
        this.deleteOldLogs();
        return ResponseEntity.ok(this.service.getLogRepository().filter(body));

    }

    private void deleteOldLogs() {
        if (AWSActionService.getDeleteSemaphore().getQueueLength() == 0) {
            this.service.deleteOldResults();
        }
    }

}
