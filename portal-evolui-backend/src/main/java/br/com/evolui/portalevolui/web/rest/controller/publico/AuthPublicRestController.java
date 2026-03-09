package br.com.evolui.portalevolui.web.rest.controller.publico;

import br.com.evolui.portalevolui.shared.dto.LoginDTO;
import br.com.evolui.portalevolui.web.beans.HealthCheckerBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.beans.enums.UserTypeEnum;
import br.com.evolui.portalevolui.web.component.JwtUtilComponent;
import br.com.evolui.portalevolui.web.repository.health_checker.HealthCheckerRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.rest.dto.monday.MondayUserDTO;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.service.MondayService;
import br.com.evolui.portalevolui.web.util.FileSaverUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Optional;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public/auth")
public class AuthPublicRestController {
    @Value("${evolui.app.externalFolder}")
    private String externalFolder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    HealthCheckerRepository healthCheckerRepository;

    @Autowired
    JwtUtilComponent jwtUtils;

    @Autowired
    MondayService mondayService;

    @PostMapping("/login")
    public ResponseEntity<LinkedHashMap> authenticateUser(@RequestBody UserBean body) throws Exception {

        Authentication authentication;
        if (body.getUserType() == UserTypeEnum.GOOGLE) {
            body = this.validateGoogleLogin(body);
            authentication = new UsernamePasswordAuthenticationToken(UserDetailsSecurity.build(body), null,
                    AuthorityUtils.createAuthorityList(body.getProfile().value()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            if (!StringUtils.hasText(body.getPassword())) {
                throw new Exception("Senha não pode estar vazia");
            }
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.getLogin(), body.getPassword()));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsSecurity userDetails = (UserDetailsSecurity) authentication.getPrincipal();
        UserBean user = this.userRepository.findById(userDetails.getId()).orElse(null);
        LinkedHashMap<String, Object> map = new LinkedHashMap();

        map.put("accessToken", jwt);
        map.put("user", user);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/login-health-checker")
    public ResponseEntity authenticateUser(@RequestBody LoginDTO<Long> body) throws Exception {
        Long id = body.getExtraInfo();
        UserBean user = null;
        if (id != null && id > 0) {
            HealthCheckerBean bean = this.healthCheckerRepository.findById(id).orElse(null);
            if (bean == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Health Checker não encontrado");
            }
            if (!bean.getUser().getLogin().equals(body.getLogin())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Não permitido");
            }

            user = bean.getUser();
        } else {
            user = this.userRepository.findByLogin(body.getLogin()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuário não encontrado");
            }
        }

        if (user.getProfile() != ProfileEnum.ROLE_HEALTHCHECKER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Perfil não permitido");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.getLogin(), body.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsSecurity userDetails = (UserDetailsSecurity) authentication.getPrincipal();
        LinkedHashMap<String, Object> map = new LinkedHashMap();

        map.put("accessToken", jwt);
        map.put("user", user);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/ldap")
    public ResponseEntity<LinkedHashMap> authenticateUser(@RequestBody LinkedHashMap<String, String> body) throws Exception {


        LinkedHashMap<String, Object> map = new LinkedHashMap();

        map.put("cpf", body.get("login"));
        return ResponseEntity.ok(map);
    }

    private UserBean validateGoogleLogin(UserBean bean) throws Exception {
        try {
            InputStream is = null;
            try {
                URL url = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + bean.getPassword());
                is = url.openStream();
                LinkedHashMap<String, String> map = new ObjectMapper().readValue(is, new TypeReference<LinkedHashMap<String, String>>() {
                });
                if (!map.get("email").equals(bean.getEmail())) {
                    throw new Exception("Usuário e/ou senha inválidos");
                }
                boolean mondayIsEnabled = this.mondayService.initialize();
                MondayUserDTO mondayUser = mondayIsEnabled ? this.mondayService.getUserByEmail(bean.getEmail()) : null;
                UserBean user = this.userRepository.findByLoginOrEmail(bean.getLogin(), bean.getEmail()).orElse(null);
                if (mondayUser != null) {
                    bean.setLogin(mondayUser.getId().toString());
                }
                if (user == null) {
                    String image = bean.getImage();
                    user = bean;
                    user.setEnabled(true);
                    if (mondayUser != null || !mondayIsEnabled) {
                        user.setProfile(ProfileEnum.ROLE_ADMIN);
                    } else {
                        user.setProfile(ProfileEnum.ROLE_USER);
                    }
                    user.setPassword(null);
                    this.userRepository.save(user);
                    user.setImage(image);
                    this.saveGoogleUserImage(user);
                } else {
                    if (mondayUser == null && mondayIsEnabled) {
                        if ((user.getProfile() == ProfileEnum.ROLE_ADMIN)) {
                            user.setProfile(ProfileEnum.ROLE_USER);
                            this.userRepository.save(user);
                        }
                    } else if (!user.getLogin().equals(bean.getLogin())) {
                        user.setProfile(ProfileEnum.ROLE_ADMIN);
                        user.setLogin(bean.getLogin());
                        this.userRepository.save(user);
                    }
                    user.setImage(bean.getImage());
                    this.saveGoogleUserImage(user);
                }
                user.setPassword(null);
                return user;

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new Exception("Usuário e/ou senha inválidos");
            } finally {
                try {
                    is.close();
                } catch (Exception ex) {

                }
            }
        }
        finally {
            this.mondayService.dispose();
        }
    }

    private Optional<String> extractEmailDomain(String emailAddress) {
        int atIdx = emailAddress.indexOf("@");
        if (atIdx > 0) {
            return Optional.of(emailAddress.substring(atIdx + 1));
        }

        return Optional.empty();
    }

    private void saveGoogleUserImage(UserBean user) {
        InputStream is = null;
        try {
            URL url = new URL(user.getImage());
            is = url.openStream();
            String folder = Paths.get(Paths.get(new URL(this.externalFolder).toURI()).toString(),
                    "users").toString();
            FileSaverUtil.saveInputStreamFile(folder, user.getId() + ".png", is);
        } catch (Exception ex) {

        } finally {
            try {
                is.close();
            } catch (Exception ex) {

            }
        }
    }

}
