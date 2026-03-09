package br.com.evolui.portalevolui.web.repository.geracao_versao;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.repository.dto.geracao_versao.GeracaoVersaoFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface GeracaoVersaoCustomRepository {
    List<GeracaoVersaoBean> filter(String produto, GeracaoVersaoFilterDTO filter) throws SQLException;
}
