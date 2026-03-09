package br.com.evolui.portalevolui.web.rest.controller.logged;

import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.component.JwtUtilComponent;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.rest.dto.UserConfigDTO;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.util.FileSaverUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/private/user")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN', 'ROLE_USER')")
public class UserLoggedRestController {

    @Autowired
    protected UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    JwtUtilComponent jwtUtils;

    @Value("${evolui.app.externalFolder}")
    private String externalFolder;

    @PostMapping()
    public ResponseEntity<LinkedHashMap> save(@RequestBody UserBean body) throws Exception {
        UserDetailsSecurity loggedUser = this.getLoggedUser();
        if (!loggedUser.getId().equals(body.getId())) {
            throw new Exception("Sem permissão");
        }
        String picture = body.getBase64Image();
        UserBean user = this.repository.findById(body.getId()).get();
        body.setPassword(user.getPassword());
        body.setProfile(user.getProfile());
        body.setEnabled(user.getEnabled());
        body = this.repository.save(body);
        body.setPassword(null);
        if (picture != null) {
            try {
                String folder = Paths.get(Paths.get(new URL(this.externalFolder).toURI()).toString(),
                        "users").toString();
                FileSaverUtil.saveBase64File(folder, body.getId() + ".png", picture);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(UserDetailsSecurity.build(body), null,
                AuthorityUtils.createAuthorityList(body.getProfile().value()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        LinkedHashMap<String, Object> map = new LinkedHashMap();

        map.put("accessToken", jwt);
        map.put("user", body);
        return ResponseEntity.ok(map);
    }

    @PostMapping("change-password")
    public ResponseEntity<UserBean> changePassword(@RequestBody UserBean body) throws Exception {
        UserDetailsSecurity loggedUser = this.getLoggedUser();
        if (!loggedUser.getId().equals(body.getId())) {
            throw new Exception("Sem permissão");
        }
        UserBean user = this.repository.findById(body.getId()).get();
        if (StringUtils.hasText(user.getPassword())) {
            if (!this.encoder.matches(body.getPassword(), user.getPassword())) {
                throw new Exception("Senha inválida");
            }
        } else if (StringUtils.hasText(body.getPassword())){
            throw new Exception("Senha inválida");
        }
        if (!StringUtils.hasText(body.getNewPassword())) {
            throw new Exception("Nova senha não pode ser vazia");
        }
        user.setPassword(encoder.encode(body.getNewPassword()));
        body = this.repository.save(user);
        body.setPassword(null);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(@RequestBody UserConfigDTO body) throws Exception {
        UserDetailsSecurity loggedUser = this.getLoggedUser();
        UserBean user = this.repository.findById(loggedUser.getId()).get();
        user.setConfig(body);
        this.repository.save(user);
        return ResponseEntity.ok(user);
    }

    private UserDetailsSecurity getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsSecurity) auth.getPrincipal();
    }



}

