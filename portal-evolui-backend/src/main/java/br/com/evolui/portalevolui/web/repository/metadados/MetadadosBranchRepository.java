package br.com.evolui.portalevolui.web.repository.metadados;

import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadadosBranchRepository extends JpaRepository<MetadadosBranchBean, Long> {
    Optional<MetadadosBranchBean> findByBranch(String branch);
    List<MetadadosBranchBean> findAllByBranchAndProjectIdentifier(String branch, String project);
    List<MetadadosBranchBean> findAllByProjectIdentifier(String project);

    @Modifying //Para não dar erro Not supported for DML operations
    @Query( "DELETE from MetadadosBranchClienteBean o where o.id = :id" )
    void deleteMetaBranchClientBeanById(@Param("id") Long id);
}

