package br.com.evolui.portalevolui.web.rest.dto.github;

import br.com.evolui.portalevolui.web.beans.AmbienteBean;
import br.com.evolui.portalevolui.web.beans.AmbienteModuloBean;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteModuloConfigDTO;
import org.springframework.beans.BeanUtils;

import java.util.LinkedHashMap;

public class GithubAmbienteDTO extends GithubVersaoDTO {
    private String identifier;
    private LinkedHashMap<String, AmbienteModuloConfigDTO> config = new LinkedHashMap<>();
    private GithubClienteDTO client;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LinkedHashMap<String, AmbienteModuloConfigDTO> getConfig() {
        return config;
    }

    public void setConfig(LinkedHashMap<String, AmbienteModuloConfigDTO> config) {
        this.config = config;
    }

    public void addConfig(String key, AmbienteModuloConfigDTO value) {
        if (this.config == null) {
            this.config = new LinkedHashMap<>();
        }
        this.config.put(key, value);
    }

    public GithubClienteDTO getClient() {
        return client;
    }

    public void setClient(GithubClienteDTO client) {
        this.client = client;
    }

    public static GithubAmbienteDTO fromBean(AmbienteBean bean) {
        if (bean == null) {
            return null;
        }
        GithubAmbienteDTO dto = new GithubAmbienteDTO();
        BeanUtils.copyProperties(bean, dto);
        if (bean.getModules() != null) {
            for (AmbienteModuloBean mod : bean.getModules()) {
                dto.addConfig(mod.getProjectModule().getIdentifier(), mod.getConfig());
            }
        }
        dto.setClient(GithubClienteDTO.fromBean(bean.getClient()));
        return dto;
    }
}
