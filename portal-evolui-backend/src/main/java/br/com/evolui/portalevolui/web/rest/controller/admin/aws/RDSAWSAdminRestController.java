package br.com.evolui.portalevolui.web.rest.controller.admin.aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.beans.enums.AWSActionTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
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
@RequestMapping("/api/admin/aws/rds")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class RDSAWSAdminRestController {

    @Autowired
    private AWSActionService service;

    @GetMapping()
    public ResponseEntity<LinkedHashMap<String, List<RDSDTO>>> get() {
        AWSService service = this.service.getAWSService();
        try {
            LinkedHashMap<String, List<RDSDTO>> resp = new LinkedHashMap<>();
            AWSConfigDTO c = service.getAllConfigs();
            if (c != null && c.getAccountConfigs() != null) {
                for (Map.Entry<String, AWSAccountConfigDTO> e : c.getAccountConfigs().entrySet()) {
                    if (service.initialize(e.getKey())) {
                        List<RDSDTO> list = service.listRds();
                        for (RDSDTO rds : list) {
                            boolean busy = this.service.isRDSBusy(rds);
                            rds.setBusy(busy);
                        }
                        resp.put(e.getKey(), list);
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
    public ResponseEntity<Void> start(@RequestBody RDSDTO body) throws Exception {
        AWSService service = this.service.getAWSService();
        try {
            service.initialize(body.getAccount());
            service.startRds(body.getId());
            LogAWSActionBean log = this.service.createLog(AWSActionTypeEnum.START_RDS, body);
            return ResponseEntity.ok(null);
        }
        finally {
            service.dispose();
        }
    }

    @PostMapping("stop")
    public ResponseEntity<Void> stop(@RequestBody RDSDTO body) throws Exception {
        AWSService service = this.service.getAWSService();
        try {
            service.initialize(body.getAccount());
            service.stoptRds(body.getId());
            LogAWSActionBean log = this.service.createLog(AWSActionTypeEnum.STOP_RDS, body);
            return ResponseEntity.ok(null);
        }
        finally {
            service.dispose();
        }

    }
}
