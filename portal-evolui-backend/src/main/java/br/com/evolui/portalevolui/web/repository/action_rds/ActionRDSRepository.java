package br.com.evolui.portalevolui.web.repository.action_rds;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;

@Repository
public interface ActionRDSRepository extends JpaRepository<ActionRDSBean, Long>, ActionRDSCustomRepository {
    List<ActionRDSBean> findAllByRequestDateBeforeAndStatus(Calendar limitDate, GithubActionStatusEnum status);
    List<ActionRDSBean> findAllBySchedulerDateAfterAndStatus(Calendar scheduler, GithubActionStatusEnum status);
    List<ActionRDSBean> findAllByStatusIn(List<GithubActionStatusEnum> status);
}
