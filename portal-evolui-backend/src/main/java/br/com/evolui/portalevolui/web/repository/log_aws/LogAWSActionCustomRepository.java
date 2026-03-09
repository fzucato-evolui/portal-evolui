package br.com.evolui.portalevolui.web.repository.log_aws;

import br.com.evolui.portalevolui.web.beans.LogAWSActionBean;
import br.com.evolui.portalevolui.web.repository.dto.LogAWSActionFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface LogAWSActionCustomRepository {
    List<LogAWSActionBean> filter(LogAWSActionFilterDTO filter) throws SQLException;
}
