package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.UserBeanFilterDTO;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.util.FileSaverUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class UsersAdminRestController {
    @Value("${evolui.app.externalFolder}")
    private String externalFolder;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @GetMapping()
    public ResponseEntity<List<UserBean>> get() {
        return ResponseEntity.ok(this.repository.findAll());

    }

    @PostMapping()
    public ResponseEntity<UserBean> save(@RequestBody UserBean body) throws Exception {
        if (body.getId() == null || body.getId() < 0) {
            if (!StringUtils.hasText(body.getPassword())) {
                throw new Exception("Senha não pode estar vazia");
            }
            body.setPassword(encoder.encode(body.getPassword()));
        } else {
            UserBean oldeUser = this.repository.findById(body.getId()).get();
            if (!StringUtils.hasText(body.getPassword())) {
                body.setPassword(oldeUser.getPassword());
            } else {
                body.setPassword(encoder.encode(body.getPassword()));
            }
            body.setUserType(oldeUser.getUserType());
        }

        this.repository.save(body);
        if (body.getBase64Image() != null) {
            try {
                String folder = Paths.get(Paths.get(new URL(this.externalFolder).toURI()).toString(),
                        "users").toString();
                FileSaverUtil.saveBase64File(folder, body.getId() + ".png", body.getBase64Image());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/filter")
    public ResponseEntity<List<UserBean>> filter(@RequestBody UserBeanFilterDTO body) throws Exception {

        return ResponseEntity.ok(this.repository.filter(body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> get(@PathVariable("id") Long id) {
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);

    }
}
