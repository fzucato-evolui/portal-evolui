package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.repository.ambiente.AmbienteRepository;
import br.com.evolui.portalevolui.web.repository.health_checker.HealthCheckerRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.AWSAccountConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubBranchDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerListDTO;
import br.com.evolui.portalevolui.web.rest.dto.monday.MondayColumnDTO;
import br.com.evolui.portalevolui.web.service.*;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/sysconfig")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class SystemConfigAdminRestController {
    @Autowired
    private SystemConfigRepository repository;

    @Autowired
    private AmbienteRepository ambienteRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private HealthCheckerRepository healthCheckerRepository;

    @Autowired
    AWSService awsService;

    @Autowired
    GithubVersionService githubService;

    @Autowired
    SMTPService smtpService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    MondayService mondayService;

    @Autowired
    CICDService cicdService;

    @Autowired
    GoogleService googleService;

    @Autowired
    AXService axService;

    @Autowired
    PortalLuthierService portalLuthierService;

    @GetMapping()
    public ResponseEntity<List<SystemConfigBean>> get() {
        return ResponseEntity.ok(this.repository.findAll());

    }

    @GetMapping("initial-data")
    public ResponseEntity<LinkedHashMap<String, Object>> getInitialData() throws Exception {
        try {
            LinkedHashMap<String, Object> data = new LinkedHashMap<>();
            data.put("configs", this.repository.findAll());
            if (this.awsService.initialize()) {
                for (Map.Entry<String, AWSAccountConfigDTO> accountConfig : this.awsService.getAllConfigs().getAccountConfigs().entrySet()) {
                    try {
                        if (accountConfig.getValue().getEnabled() != null && accountConfig.getValue().getEnabled().booleanValue()) {
                            this.awsService.initialize(accountConfig.getKey());
                            if (accountConfig.getValue().getMain() != null && accountConfig.getValue().getMain().booleanValue()) {
                                data.put("ec2DEV", this.awsService.listEc2());
                                data.put("wksDEV", this.awsService.listWorkspaces());
                            }
                            if (data.get("rds") == null) {
                                data.put("rds", new ArrayList<RDSDTO>());
                            }
                            ((ArrayList<RDSDTO>) data.get("rds")).addAll(this.awsService.listRds());
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (this.githubService.initialize()) {
                try {
                    GithubRunnerListDTO runners = this.githubService.getRunners();
                    if (runners != null && runners.getTotal_count() > 0) {
                        data.put("runners", runners.getRunners());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (this.googleService.initialize()) {
                try {
                    data.put("spaces", this.googleService.getSpaces());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (this.mondayService.initialize()) {
                try {
                    data.put("boards", this.mondayService.listBoards());
                    try {
                        data.put("boardBuildGroups", this.mondayService.listGroupsBoard());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        data.put("boardBuildColumns", this.mondayService.listColumnsBoard());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        data.put("boardTaskColumns", this.mondayService.listColumnsTaskBoard());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            data.put("environments", this.ambienteRepository.findAllByOrderByProjectIdentifierAscIdentifierAsc());
            data.put("healthcheckers", this.healthCheckerRepository.findAllByOrderByIdentifierAsc());
            List<ProjectBean> projects = this.projectRepository.findAll();
            if (projects != null && projects.size() > 0) {
                LinkedHashMap<Long, List<String>> branches = new LinkedHashMap<>();
                try {
                    for (ProjectBean p : projects) {
                        List<GithubBranchDTO> bs = this.githubService.getAllBranches(p.getRepository());
                        branches.put(p.getId(), bs.stream().map(x -> x.getName()).collect(Collectors.toList()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                data.put("productBranches", branches);
            }


            return ResponseEntity.ok(data);
        }
        finally {
            this.awsService.dispose();
            this.githubService.dispose();
            this.mondayService.dispose();
            this.googleService.refresh();
        }

    }

    @PostMapping()
    @JsonView(JsonViewerPattern.Admin.class)
    public ResponseEntity<SystemConfigBean> save(@RequestBody SystemConfigBean body) {
        this.repository.save(body);
        if (body.getConfigType() == SystemConfigTypeEnum.AWS) {
            this.awsService.dispose();
        } else if (body.getConfigType() == SystemConfigTypeEnum.GITHUB) {
            this.githubService.dispose();
        } else if (body.getConfigType() == SystemConfigTypeEnum.SMTP) {
            this.smtpService.dispose();
        } else if (body.getConfigType() == SystemConfigTypeEnum.NOTIFICATION) {
            this.notificationService.dispose();
        } else if (body.getConfigType() == SystemConfigTypeEnum.MONDAY) {
            this.mondayService.dispose();
        } else if (body.getConfigType() == SystemConfigTypeEnum.GOOGLE) {
            this.googleService.refresh();
        } else if (body.getConfigType() == SystemConfigTypeEnum.CICD) {
            this.cicdService.refresh();
        } else if (body.getConfigType() == SystemConfigTypeEnum.AX) {
            this.axService.dispose();
//            try {
//                this.axService.notifyVersionGeneration(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        else if (body.getConfigType() == SystemConfigTypeEnum.PORTAL_LUTHIER) {
            this.portalLuthierService.dispose();
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("email-test/{destination}")
    public ResponseEntity<Void> sendEmailTest(@PathVariable("destination") String destination) throws Exception {
        this.smtpService.sendTest(destination);
        return ResponseEntity.ok(null);

    }

    @GetMapping("monday-build-groups-columns/{boardId}")
    public ResponseEntity<LinkedHashMap> getMondayBuildBoardGroupsAndColumns(@PathVariable("boardId") String boardId) throws Exception {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("boardBuildGroups", this.mondayService.listGroupsBoard(boardId));
        data.put("boardBuildColumns", this.mondayService.listColumnsBoard(boardId, null, null));
        return ResponseEntity.ok(data);

    }

    @GetMapping("monday-task-columns/{boardId}")
    public ResponseEntity<LinkedHashMap> getMondayTaskBoardColumns(@PathVariable("boardId") String boardId) throws Exception {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("boardTaskColumns", this.mondayService.listColumnsBoard(boardId, null, null));
        return ResponseEntity.ok(data);

    }

    @GetMapping("monday-column/{boardId}/{columnId}")
    public ResponseEntity<MondayColumnDTO> getMondayColumns(@PathVariable("boardId") String boardId, @PathVariable("columnId") String columnId) throws Exception {
        List<MondayColumnDTO> columns = this.mondayService.listColumnsBoard(boardId, Arrays.asList(columnId), null);
        if (columns != null && !columns.isEmpty()) {
            return ResponseEntity.ok(columns.get(0));
        }
        return ResponseEntity.ok(null);

    }
}
