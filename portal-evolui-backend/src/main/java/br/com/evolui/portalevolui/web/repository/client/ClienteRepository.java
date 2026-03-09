package br.com.evolui.portalevolui.web.repository.client;

import br.com.evolui.portalevolui.web.beans.ClienteBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteBean, Long> {
    List<ClienteBean> findAllByProjectId(Long id);
    List<ClienteBean> findAllByProjectIdentifier(String identifier);
}
