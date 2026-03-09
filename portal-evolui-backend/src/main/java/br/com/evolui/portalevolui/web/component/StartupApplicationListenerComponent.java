package br.com.evolui.portalevolui.web.component;

import br.com.evolui.portalevolui.web.service.AWSActionService;
import br.com.evolui.portalevolui.web.service.AtualizacaoVersaoService;
import br.com.evolui.portalevolui.web.service.CICDService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StartupApplicationListenerComponent {

    @Autowired
    private CICDService cicdService;

    @Autowired
    private AtualizacaoVersaoService atualizacaoVersaoService;

    @Autowired
    private AWSActionService awsActionService;

    @PostConstruct
    public void init() {
        try {
            this.cicdService.refresh();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        try {
            this.atualizacaoVersaoService.initScheduled();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        try {
            this.awsActionService.initScheduled();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
