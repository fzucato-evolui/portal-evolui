package br.com.evolui.portalevolui.web.repository.health_checker;

import br.com.evolui.portalevolui.web.beans.HealthCheckerBean;
import br.com.evolui.portalevolui.web.beans.HealthCheckerModuloBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthCheckerRepository extends JpaRepository<HealthCheckerBean, Long> {
    @Query("SELECT u FROM UserBean u LEFT JOIN HealthCheckerBean h ON h.user = u " +
            "WHERE u.profile=br.com.evolui.portalevolui.web.beans.enums.ProfileEnum.ROLE_HEALTHCHECKER AND h.id IS NULL")
    List<UserBean> findAllPossibleUsers();

    Optional<HealthCheckerBean> findByUserId(Long id);
    Optional<HealthCheckerBean> findByUserIdAndIdNot(Long userId, Long id);

    @Modifying //Para não dar erro Not supported for DML operations
    @Query( "DELETE FROM HealthCheckerModuloBean m WHERE m.id = :id" )
    void deleteModuleById(@Param("id") Long id);

    @Query("SELECT m FROM HealthCheckerModuloBean m WHERE m.id = :id")
    Optional<HealthCheckerModuloBean> findModuleById(@Param("id") Long id);

    List<HealthCheckerBean> findAllByOrderByIdentifierAsc();
}
