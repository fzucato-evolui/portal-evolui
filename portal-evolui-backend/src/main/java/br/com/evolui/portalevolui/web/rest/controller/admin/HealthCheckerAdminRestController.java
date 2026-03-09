package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.shared.dto.*;
import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.shared.util.GeradorTokenPortalEvolui;
import br.com.evolui.portalevolui.web.beans.HealthCheckerBean;
import br.com.evolui.portalevolui.web.beans.HealthCheckerModuloBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.config.WebSocketConfig;
import br.com.evolui.portalevolui.web.repository.health_checker.HealthCheckerRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.service.NotificationService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/health-checker")
public class HealthCheckerAdminRestController {

    @Autowired
    HealthCheckerRepository repository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @JsonView(JsonViewerPattern.Admin.class)
    @GetMapping("/all")
    public ResponseEntity<List<HealthCheckerBean>> getAll() {
        return ResponseEntity.ok(this.repository.findAll());
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<HealthCheckerBean> get(@PathVariable("id") Long id) {
        HealthCheckerBean bean = this.repository.findById(id).get();
        HealthCheckerConfigDTO dto = bean.getConfig();
        dto.setId(id);
        if (dto.getModules() != null) {
            dto.getModules().clear();
        }
        dto.setSystemInfo(bean.getSystemInfo());
        for (HealthCheckerModuloBean mod : bean.getModules()) {
            mod.getConfig().setId(mod.getId());
            dto.addModule(mod.getConfig());
        }
        bean.setConfig(dto);

        return ResponseEntity.ok(bean);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @GetMapping("/alerts/{id}")
    public ResponseEntity<List<HealthCheckerAlertDTO>> getAlerts(@PathVariable("id") Long id) {
        HealthCheckerBean bean = this.repository.findById(id).get();
        return ResponseEntity.ok(bean.getAlerts());
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @GetMapping("/alerts-module/{id}")
    public ResponseEntity<List<HealthCheckerAlertDTO>> getAlertsModule(@PathVariable("id") Long id) {
        HealthCheckerModuloBean bean = this.repository.findModuleById(id).get();
        HealthCheckerAlertDTO alert = new HealthCheckerModuleDTO();
        alert.setError(bean.getAlerts());
        return ResponseEntity.ok(Arrays.asList(alert));
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @GetMapping("/possible-users")
    public ResponseEntity<List<UserBean>> getPossibleUsers() {
        return ResponseEntity.ok(this.repository.findAllPossibleUsers());
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @PostMapping("/generate-token")
    public ResponseEntity<LinkedHashMap<String, Object>> generateToken(@RequestBody UserBean body) throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        UserBean user = this.userRepository.findByLogin(body.getLogin()).get();
        if (!this.encoder.matches(body.getPassword(), user.getPassword())) {
            throw new Exception("Senha inválida");
        }
        if (user.getProfile() != ProfileEnum.ROLE_HEALTHCHECKER) {
            throw new Exception("Não é um usuário HealthChecker");
        }
        if(this.repository.findByUserIdAndIdNot(user.getId(), body.getId()).isPresent()) {
            throw new Exception("Usuário já está sendo usado em outro HealthChecker");
        }
        HealthCheckerConnectionDTO token = new HealthCheckerConnectionDTO();
        token.setDestination(body.getName()); // Usado pra passar o jwt token de identificação
        LoginDTO login = new LoginDTO();
        login.setLogin(body.getLogin());
        login.setPassword(body.getPassword());
        token.setLogin(login);
        token.setHost(this.baseUrl + ":" + this.port);

        LinkedHashMap resp = new LinkedHashMap();
        resp.put("token", GeradorTokenPortalEvolui.encrypt(mapper.writeValueAsString(token)));
        resp.put("endpoint", token.getHost());
        if (StringUtils.hasText(body.getNewPassword())) { // Usado para passar o identificador do healthchecker
            resp.put("online", WebSocketConfig.connectedDevices.containsKey(body.getNewPassword()));
        }

        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @PostMapping("/test-certificate")
    public ResponseEntity<LinkedHashMap<String, Object>> generateToken(@RequestBody CertificateDTO body) throws Exception{
        KeyStore store = body.getKeystore();
        Enumeration aliases = store.aliases();
        String alias = (String) aliases.nextElement();
        Certificate trustedcert = store.getCertificate(alias);
        LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
        if (trustedcert != null && trustedcert instanceof X509Certificate) {
            X509Certificate cert = (X509Certificate) trustedcert;
            resp.put("subject", cert.getSubjectDN().toString());
            resp.put("signatureAlgorithm", cert.getSigAlgName());
            resp.put("validFrom", cert.getNotBefore());
            resp.put("validUntil", cert.getNotAfter());
            resp.put("issuer", cert.getIssuerDN().toString());
        }

        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping()
    public ResponseEntity<HealthCheckerConfigDTO> save(@RequestBody HealthCheckerConfigDTO body) throws Exception{
        HealthCheckerBean bean = null;
        if (body.getId() != null && body.getId() > 0) {
            bean = this.repository.findById(body.getId()).get();
        } else {
            bean = new HealthCheckerBean();
        }
        UserBean user = this.userRepository.findByLogin(body.getLogin().getLogin()).get();
        if (user.getProfile() != ProfileEnum.ROLE_HEALTHCHECKER) {
            throw new Exception("Não é um usuário HealthChecker");
        }
        if(this.repository.findByUserIdAndIdNot(user.getId(), bean.getId()).isPresent()) {
            throw new Exception("Usuário já está sendo usado em outro HealthChecker");
        }
        bean.setUser(user);
        bean.setIdentifier(body.getIdentifier());
        bean.setDescription(body.getDescription());
        bean.setSystemInfo(body.getSystemInfo());
        // Para não salvar informações que já estarão em outros campos
        HealthCheckerConfigDTO savedConfig = HealthCheckerConfigDTO.fromJson(body.toJson());
        savedConfig.setModules(null);
        savedConfig.setSystemInfo(null);
        savedConfig.getLogin().setPassword(null);
        bean.setConfig(savedConfig);

        if (body.getId() != null && body.getId() > 0) {
            if (bean.getModules() != null && !bean.getModules().isEmpty()) {
                for(int i = 0; i < bean.getModules().size();) {
                    HealthCheckerModuloBean mod = bean.getModules().get(i);
                    Long id = mod.getId();
                    if (body.getModules() == null || body.getModules().isEmpty() ||
                            !body.getModules().stream().anyMatch(x -> x.getId() != null && x.getId().equals(id))) {
                        this.repository.deleteModuleById(id);
                        bean.getModules().remove(i);
                        continue;
                    }
                    i++;
                }
            }
        }
        if (body.getModules() != null && !body.getModules().isEmpty()) {
            for (HealthCheckerModuleConfigDTO moduleConfig : body.getModules()) {
                HealthCheckerModuloBean modulo = bean.getModules() != null ?
                        bean.getModules().stream().filter(
                                x -> x.getIdentifier().equals(moduleConfig.getIdentifier())).findFirst().orElse(null) : null;
                if (modulo == null) {
                    modulo = new HealthCheckerModuloBean();
                    bean.addModule(modulo);
                }
                modulo.setConfig(moduleConfig);
                modulo.setDescription(moduleConfig.getDescription());
                modulo.setIdentifier(moduleConfig.getIdentifier());
            }
        }

        bean = this.repository.save(bean);
        body.setId(bean.getId());
        if (body.getModules() != null && !body.getModules().isEmpty()) {
            for (HealthCheckerModuleConfigDTO moduleConfig : body.getModules()) {
                HealthCheckerModuloBean modulo = bean.getModules().stream().filter(
                        x -> x.getIdentifier().equals(moduleConfig.getIdentifier())).findFirst().get();
                moduleConfig.setId(modulo.getId());
            }
        }
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws Exception {

        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }

    @PreAuthorize("hasAnyRole('ROLE_HEALTHCHECKER')")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/check")
    public ResponseEntity save(@RequestBody HealthCheckerDTO body) throws Exception {
        Calendar now = Calendar.getInstance();
        Optional<HealthCheckerBean> opbean = this.repository.findById(body.getId());
        if (!opbean.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("HealthChecker não encontrado");
        }
        HealthCheckerBean bean = opbean.get();
        ObjectMapper mapper = new ObjectMapper();
        HealthCheckerBean previousBean = mapper.readValue(mapper.writeValueAsString(bean), HealthCheckerBean.class);
        UserBean user = this.getLoggedUser();
        if (user.getProfile() != ProfileEnum.ROLE_HEALTHCHECKER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Não é um usuário HealthChecker");
        }
        if(!bean.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuário incorreto");
        }

        bean.setLastUpdate(now);
        if (body.getAlerts() != null && !body.getAlerts().isEmpty()) {
            bean.setHealth(false);
            bean.setAlerts(body.getAlerts());
        } else {
            bean.setHealth(true);
            bean.setAlerts(null);
            bean.setLastHealthDate(now);
        }
        if (body.getModules() != null && !body.getModules().isEmpty()) {
            for (HealthCheckerModuleDTO module : body.getModules()) {
                HealthCheckerModuloBean moduloBean = bean.getModules().stream().filter(x -> x.getId().equals(module.getId())).findFirst().orElse(null);
                if (moduloBean != null) {
                    moduloBean.setHealth(module.isHealth());
                    moduloBean.setLastUpdate(now);
                    if (!module.isHealth()) {
                        moduloBean.setAlerts(module.getError());
                    } else {
                        moduloBean.setAlerts(null);
                        moduloBean.setLastHealthDate(now);
                    }
                }
            }
        }

        bean = this.repository.save(bean);

        try {
            this.notificationService.sendHealthCheckerAsync(previousBean, bean, this.baseUrl + ":" + this.port + "/admin/health-checker/" + bean.getId());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return ResponseEntity.ok(body);
    }

    private UserBean getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsSecurity user = (UserDetailsSecurity) auth.getPrincipal();
        return this.userRepository.findById(user.getId()).get();
    }
}
