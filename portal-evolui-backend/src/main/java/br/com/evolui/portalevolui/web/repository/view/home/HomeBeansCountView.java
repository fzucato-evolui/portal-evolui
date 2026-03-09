package br.com.evolui.portalevolui.web.repository.view.home;

import br.com.evolui.portalevolui.web.repository.enums.home.HomeBeanCountTypeEnum;

public interface HomeBeansCountView {
    Long getTotal();
    String getValor();
    HomeBeanCountTypeEnum getCountType();
    Long getProdutoId();
}
