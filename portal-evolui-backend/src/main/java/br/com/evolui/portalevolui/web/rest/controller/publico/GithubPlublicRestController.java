package br.com.evolui.portalevolui.web.rest.controller.publico;

import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubCICDResultDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubGeracaoVersaoResultDTO;
import br.com.evolui.portalevolui.web.service.AtualizacaoVersaoService;
import br.com.evolui.portalevolui.web.service.CICDService;
import br.com.evolui.portalevolui.web.service.GeracaoVersaoSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public/github")
public class GithubPlublicRestController {
    @Autowired
    GeracaoVersaoSchedulerService service;

    @Autowired
    AtualizacaoVersaoService updateService;

    @Autowired
    CICDService cicdService;

    @PostMapping("/webhook-geracao-versao/{produto}/{token}")
    public ResponseEntity<Void> receiveWebhook(@PathVariable("produto") String produto, @PathVariable("token") String token, @RequestBody GithubGeracaoVersaoResultDTO body) throws Exception {
        body.setStatus(GithubActionStatusEnum.completed);
        body.setConclusionDate(Calendar.getInstance());
        this.service.searchPending(produto, token, body);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/webhook-atualizacao-versao/{produto}/{token}")
    public ResponseEntity<Void> receiveWebhookUpate(@PathVariable("produto") String produto, @PathVariable("token") String token, @RequestBody GithubGeracaoVersaoResultDTO body) throws Exception {
        body.setStatus(GithubActionStatusEnum.completed);
        body.setConclusionDate(Calendar.getInstance());
        this.updateService.searchPending(produto, token, body);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/webhook-cicd/{produto}/{branch}/{token}")
    public ResponseEntity<Void> receiveWebhookCICD(@PathVariable("produto") String produto,
                                                   @PathVariable("produto") String branch,
                                                   @PathVariable("token") String token,
                                                   @RequestBody GithubCICDResultDTO body) throws Exception {
        body.setStatus(GithubActionStatusEnum.completed);
        body.setConclusionDate(Calendar.getInstance());
        this.cicdService.searchPending(produto, branch, token, body);
        return ResponseEntity.ok(null);
    }

}
