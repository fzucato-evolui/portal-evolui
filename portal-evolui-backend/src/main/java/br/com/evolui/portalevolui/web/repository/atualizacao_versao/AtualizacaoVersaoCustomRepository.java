package br.com.evolui.portalevolui.web.repository.atualizacao_versao;

import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.repository.dto.atualizacao_versao.AtualizacaoVersaoFilterDTO;

import java.sql.SQLException;
import java.util.List;

public interface AtualizacaoVersaoCustomRepository {
    List<AtualizacaoVersaoBean> filter(String produto, AtualizacaoVersaoFilterDTO filter) throws SQLException;
}
