package br.com.evolui.portalevolui.web.repository;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfigBean, Long> {
    @Transactional(readOnly = true)
    Optional<SystemConfigBean> findByConfigType(SystemConfigTypeEnum configType);

    @Transactional(readOnly = true)
    List<SystemConfigBean> findAllByConfigTypeIn(List<SystemConfigTypeEnum> configTypes);
}
