package br.com.evolui.portalevolui.web.repository.cicd;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.repository.dto.cicd.CICDFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface CICDCustomRepository {
    List<CICDBean> filter(String produto, CICDFilterDTO filter) throws SQLException;
}
