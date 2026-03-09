package br.com.evolui.portalevolui.web.repository.ambiente;

import br.com.evolui.portalevolui.web.beans.AmbienteBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmbienteRepository extends JpaRepository<AmbienteBean, Long> {
    List<AmbienteBean> findAllByProjectIdentifier(String project);
    List<AmbienteBean> findAllByBranchAndProjectIdentifier(String branch, String project);
    List<AmbienteBean> findAllByProjectIdentifierOrderByIdentifierAsc(String project);
    List<AmbienteBean> findAllByOrderByProjectIdentifierAscIdentifierAsc();
}

