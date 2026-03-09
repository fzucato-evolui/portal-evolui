package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.ClienteBean;
import br.com.evolui.portalevolui.web.repository.client.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
@RequestMapping("/api/admin/cliente")
public class ClienteAdminRestController {

    private String target;
    @Autowired
    ClienteRepository repository;
    @GetMapping("/{produto}/all")
    public ResponseEntity<List<ClienteBean>> getAll(@PathVariable("produto") String produto) {
        this.target = produto;
        return ResponseEntity.ok(this.repository.findAllByProjectIdentifier(produto));
    }

    @GetMapping("/{produto}/{id}")
    public ResponseEntity<ClienteBean> get(@PathVariable("produto") String produto, @PathVariable("id") Long id) {
        this.target = produto;
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @PostMapping("/{produto}")
    public ResponseEntity<ClienteBean> save(@PathVariable("produto") String produto, @RequestBody ClienteBean body) throws Exception{
        this.target = produto;
        return ResponseEntity.ok(this.repository.save(body));
    }

    @DeleteMapping("/{produto}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        this.target = produto;
        this.repository.deleteById(id);
        return ResponseEntity.ok(null);
    }
}
