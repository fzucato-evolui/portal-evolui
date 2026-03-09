package br.com.evolui.portalevolui.web.repository.action_rds;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.repository.dto.ActionRDSFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface ActionRDSCustomRepository {
    List<ActionRDSBean> filter(ActionRDSFilterDTO filter) throws SQLException;
}
