package br.com.evolui.portalevolui.web.rest.controller.admin.aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.beans.enums.AWSActionTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.aws.WorkspaceDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSAccountConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSConfigDTO;
import br.com.evolui.portalevolui.web.service.AWSActionService;
import br.com.evolui.portalevolui.web.service.AWSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/aws/workspace")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER')")
public class WorkspaceAWSAdminRestController {

    @Autowired
    private AWSActionService service;

    @GetMapping()
    public ResponseEntity<LinkedHashMap<String, List<WorkspaceDTO>>> get() {
        AWSService service = this.service.getAWSService();
        try {
            LinkedHashMap<String, List<WorkspaceDTO>> resp = new LinkedHashMap<>();
            AWSConfigDTO c = service.getAllConfigs();
            if (c != null && c.getAccountConfigs() != null) {
                for (Map.Entry<String, AWSAccountConfigDTO> e : c.getAccountConfigs().entrySet()) {
                    if (service.initialize(e.getKey())) {
                        resp.put(e.getKey(), service.listWorkspaces());
                    }
                }
            }

            return ResponseEntity.ok(resp);
        }
        finally {
            service.dispose();
        }
    }

    @PostMapping("start")
    public ResponseEntity<Void> start(@RequestBody WorkspaceDTO body) throws Exception {
        AWSService service = this.service.getAWSService();
        try {
            service.initialize(body.getAccount());
            service.startWorkspace(body.getId());
            LogAWSActionBean log = this.service.createLog(AWSActionTypeEnum.START_WORKSPACE, body);

            return ResponseEntity.ok(null);
        }
        finally {
            service.dispose();
        }

    }

    @PostMapping("stop")
    public ResponseEntity<Void> stop(@RequestBody WorkspaceDTO body) throws Exception {
        AWSService service = this.service.getAWSService();
        try {
            service.initialize(body.getAccount());
            service.stopWorkspace(body.getId());
            LogAWSActionBean log = this.service.createLog(AWSActionTypeEnum.STOP_WORKSPACE, body);

            return ResponseEntity.ok(null);
        }
        finally {
            service.dispose();
        }

    }

    @PostMapping("reboot")
    public ResponseEntity<Void> restart(@RequestBody WorkspaceDTO body) throws Exception {
        AWSService service = this.service.getAWSService();
        try {
            service.initialize(body.getAccount());
            service.rebootWorkspace(body.getId());
            LogAWSActionBean log = this.service.createLog(AWSActionTypeEnum.REBOOT_WORKSPACE, body);

            return ResponseEntity.ok(null);
        }
        finally {
            service.dispose();
        }

    }
}
