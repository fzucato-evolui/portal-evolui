package br.com.evolui.portalevolui.web.rest.intefaces;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;

public interface ISystemConfigService {
    boolean initialize(Object... param);
    void dispose();

    SystemConfigBean getSystemConfig();
}
