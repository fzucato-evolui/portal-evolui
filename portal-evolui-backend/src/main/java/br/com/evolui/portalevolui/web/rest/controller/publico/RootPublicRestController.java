package br.com.evolui.portalevolui.web.rest.controller.publico;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.rest.dto.RootDTO;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public/root")
public class RootPublicRestController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @GetMapping()
    @JsonView(JsonViewerPattern.Public.class)
    public ResponseEntity<RootDTO> get() throws Exception {

        RootDTO dto = new RootDTO();
        UserBean user = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            UserDetailsSecurity userPrincipal = (UserDetailsSecurity) auth.getPrincipal();
            user = this.userRepository.findById(userPrincipal.getId()).orElse(null);
        }
        dto.setConfigs(this.systemConfigRepository.findAllByConfigTypeIn(Arrays.asList(SystemConfigTypeEnum.GOOGLE)));
        dto.setUser(user);
        return ResponseEntity.ok(dto);

    }

}