package br.com.evolui.portalevolui.web.repository.user;

import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.dto.UserBeanFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface UserCustomRepository {
    List<UserBean> filter(UserBeanFilterDTO filter) throws SQLException;
}
