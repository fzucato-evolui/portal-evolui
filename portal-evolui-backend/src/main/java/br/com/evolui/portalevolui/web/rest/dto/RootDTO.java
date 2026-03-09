package br.com.evolui.portalevolui.web.rest.dto;

import br.com.evolui.portalevolui.shared.json.pattern.JsonViewerPattern;
import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.config.GoogleConfigDTO;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.ArrayList;
import java.util.List;

@JsonView(JsonViewerPattern.Public.class)
public class RootDTO {
    private UserBean user;
    private List<SystemConfigBean> configs = new ArrayList<>();

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public List<SystemConfigBean> getConfigs() {
        return configs;
    }

    public void setConfigs(List<SystemConfigBean> configs) {
        this.configs = configs;
    }

    private void removeSecretsConfigs() {
        if (this.configs != null) {
            for (SystemConfigBean c : this.configs) {
                if (c.getConfigType() == SystemConfigTypeEnum.GOOGLE) {
                    GoogleConfigDTO dto = (GoogleConfigDTO)c.getConfig();
                    dto.setClientID(null);
                    dto.setSecretKey(null);
                }
            }
        }
    }
}
