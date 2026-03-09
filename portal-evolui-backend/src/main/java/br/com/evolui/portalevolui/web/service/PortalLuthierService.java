package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.PortalLuthierConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.portal_luthier.PortalLuthierContextDTO;
import br.com.evolui.portalevolui.web.rest.dto.portal_luthier.PortalLuthierLoginResponseDTO;
import br.com.evolui.portalevolui.web.rest.dto.portal_luthier.PortalLuthierUserDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class PortalLuthierService implements ISystemConfigService {
    @Autowired
    private SystemConfigRepository configRepository;
    @Autowired
    private ObjectMapper mapper;

    private PortalLuthierConfigDTO config;

    private PortalLuthierUserDTO user;

    private String accessToken;

    @Override
    public boolean initialize(Object... param) {
        return this.getConfig() != null && this.getConfig().getEnabled() != null && this.getConfig().getEnabled();
    }

    @Override
    public void dispose() {
        this.config = null;
    }

    @Override
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.PORTAL_LUTHIER).orElse(null);
    }

    private void login(PortalLuthierConfigDTO config) throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl(this.config.getServer())
                .pathSegment("api", "public", "auth", "login")
                .toUriString();
        PortalLuthierUserDTO user = new PortalLuthierUserDTO();
        user.setLogin(config.getUser());
        user.setPassword(config.getPassword());
        RestClientService restClientService = RestClientService.using(url, null);
        String json = restClientService.doRequest(HttpMethod.POST, user);

        PortalLuthierLoginResponseDTO result =
                this.mapper.readValue(json, PortalLuthierLoginResponseDTO.class);

        this.accessToken = result.getAccessToken();
        this.user = result.getUser();

    }

    public List<PortalLuthierContextDTO> getContexts() throws Exception {
        PortalLuthierConfigDTO config = this.getConfig();
        if (config == null || config.getEnabled() == null || !config.getEnabled()) {
            return null;
        }
        this.login(config);
        String url = UriComponentsBuilder
                .fromHttpUrl(this.config.getServer())
                .pathSegment("api", "external", "luthier4J", "all-contexts")
                .toUriString();
        RestClientService restClientService = RestClientService.using(url, true, this.accessToken);
        String json = restClientService.doRequest(HttpMethod.GET, null);
        List<PortalLuthierContextDTO> contexts = this.mapper.readValue(
                json,
                new TypeReference<List<PortalLuthierContextDTO>>() {}
        );
        if (contexts != null && !contexts.isEmpty()) {
            contexts.forEach(context -> {
                context.setLuthierUser(config.getLuthierUser());
                context.setLuthierPassword(config.getLuthierPassword());
            });
        }
        return contexts;
    }

    public PortalLuthierConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (PortalLuthierConfigDTO) c.getConfig();
            }
            else {
                this.config = new PortalLuthierConfigDTO();
            }
        }
        return config;
    }

    public void setConfig(PortalLuthierConfigDTO dto) {
        this.config = dto;
    }

    public PortalLuthierUserDTO getUser() {
        return user;
    }
}
