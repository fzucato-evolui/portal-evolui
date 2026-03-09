package br.com.evolui.portalevolui.web.repository.log_aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;

@Repository
public interface LogAWSActionRepository extends JpaRepository<LogAWSActionBean, Long>, LogAWSActionCustomRepository {
    List<LogAWSActionBean> findAllByLogDateBefore(Calendar limitDate);
}
