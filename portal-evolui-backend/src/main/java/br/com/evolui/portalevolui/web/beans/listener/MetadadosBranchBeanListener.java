package br.com.evolui.portalevolui.web.beans.listener;

import br.com.evolui.portalevolui.web.beans.MetadadosBranchBean;
import br.com.evolui.portalevolui.web.repository.metadados.MetadadosBranchRepository;
import br.com.evolui.portalevolui.web.service.BeanUtilService;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class MetadadosBranchBeanListener {
    private MetadadosBranchRepository repository;

    @PrePersist
    @PreUpdate
    private void beforeAnyUpdate(MetadadosBranchBean body) {
        if (body.getId() == null || body.getId() <= 0) {
            return;
        }
        this.repository = BeanUtilService.getBean(MetadadosBranchRepository.class);
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        MetadadosBranchBean bean = this.repository.findById(body.getId()).orElse(null);
        /*
        if (bean.getMetaClients() == null || bean.getMetaClients().isEmpty()) {
            return;
        }
        for(MetadadosBranchClienteBean metaClient: bean.getMetaClients()) {
            Long idClient = metaClient.getClient().getId();
            Long idMetaClient = metaClient.getId();
            MetadadosBranchClienteBean metaBody = body.getMetaClients().stream().filter(x -> x.getClient().getId().equals(idClient)).findFirst().orElse(null);
            if (metaBody == null) {
                this.repository.deleteMetaBranchClientBeanById(idMetaClient);
            } else {
                metaBody.setId(idMetaClient);
            }
        }

         */


    }
}
