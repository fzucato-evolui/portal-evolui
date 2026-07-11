package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.AXConfigDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AXService implements ISystemConfigService {
    private static final Logger logger = LoggerFactory.getLogger(AXService.class);
    @Autowired
    private SystemConfigRepository configRepository;

    private AXConfigDTO config;

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
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.AX).orElse(null);
    }

    public void test(GeracaoVersaoBean bean) throws Exception {
        AXConfigDTO config = this.getConfig();
        if (config == null || config.getEnabled() == null || !config.getEnabled()) {
            return;
        }
        String url = UriComponentsBuilder
                .fromHttpUrl(this.config.getServer())
                .pathSegment("api", "ax-external", "tasks", "userTasksList")
                .queryParam("email", this.config.getUser())
                .toUriString();
        RestClientService restClientService = RestClientService.using(url, true, this.config.getToken());
        String json = restClientService.doRequest(HttpMethod.GET, null);
        System.out.println("json: " + json);
    }

    @Async
    public void notifyVersionGenerationAsync(GeracaoVersaoBean bean) throws Exception {
        try {
            logger.info("Enviando notificação geração versão AX: Projeto: {}, Tipo: {}, Versão: {}",
                    bean.getProject().getIdentifier(),
                    bean.getCompileType().value(),
                    bean.getTag());
            AXConfigDTO config = this.getConfig();
            if (config == null || config.getEnabled() == null || !config.getEnabled()) {
                return;
            }
            String url = UriComponentsBuilder
                    .fromHttpUrl(this.config.getServer())
                    .pathSegment("api", "idp", "version-generation", "callBack")
                    .queryParam("email", this.config.getUser())
                    .toUriString();
            logger.debug("URL AX : {}", url);
            RestClientService restClientService = RestClientService.using(url, true, this.config.getToken());
            String json = restClientService.doRequest(HttpMethod.POST, bean);
            logger.info("Requisição AX geração versão enviada. Resp: {}", json);
        } catch (Exception e) {
            logger.error("Erro ao enviar requisição geração versão AX: {}", e.getMessage());
            throw e;
        }
    }

    public AXConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (AXConfigDTO) c.getConfig();
            }
            else {
                this.config = new AXConfigDTO();
            }
        }
        return config;
    }

    public void setConfig(AXConfigDTO dto) {
        this.config = dto;
    }
}
