package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.AmbienteModuloBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoBean;
import br.com.evolui.portalevolui.web.beans.AtualizacaoVersaoModuloBean;
import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteModuloConfigDTO;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.repository.dto.atualizacao_versao.AtualizacaoVersaoFilterDTO;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.rest.dto.version.AvailableVersionDTO;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import br.com.evolui.portalevolui.web.service.AtualizacaoVersaoService;
import br.com.evolui.portalevolui.web.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/atualizacao-versao")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class AtualizacaoVersaoAdminRestController {
    
    private String target;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    AtualizacaoVersaoService service;

    @Autowired
    ProjectRepository projectRepository;

    @GetMapping("/{produto}/all")
    public ResponseEntity<List<AtualizacaoVersaoBean>> getAll(@PathVariable("produto") String produto) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findAllByEnvironmentProjectIdentifier(produto));
    }

    @GetMapping("/{produto}/{id}")
    public ResponseEntity<AtualizacaoVersaoBean> get(@PathVariable("produto") String produto, @PathVariable("id") Long id) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
    }

    @PostMapping("/{produto}/filter")
    public ResponseEntity<List<AtualizacaoVersaoBean>> filter(@PathVariable("produto") String produto, @RequestBody AtualizacaoVersaoFilterDTO body) throws Exception{
        this.deleteOldResults();
        this.target = produto;
        long pendings = this.service.getRepository().countByStatusNotAndEnvironmentProjectIdentifierAndWorkflowIsNotNull(GithubActionStatusEnum.completed, produto);
        if (pendings > 0) {
            this.searchPendingVersion();
        }
        return ResponseEntity.ok(this.service.getRepository().filter(produto, body));

    }

    @PostMapping("/{produto}")
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<AtualizacaoVersaoBean> generate(@PathVariable("produto") String produto, @RequestBody AtualizacaoVersaoBean body) throws Exception{
        this.target = produto;
        body.setRequestDate(Calendar.getInstance());
        body.setConclusion(null);
        body.setUser(this.getLoggedUser());
        body.setHashToken(EncryptionUtil.generateToken(this.target));
        return ResponseEntity.ok(this.service.generate(body));

    }

    @GetMapping("/{produto}/cancel/{id}")
    public ResponseEntity<AtualizacaoVersaoBean> cancel(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = produto;
            AtualizacaoVersaoBean bean = this.service.getRepository().findById(id).get();
            if (bean.getStatus() == GithubActionStatusEnum.completed) {
                throw new Exception("A requisição já está processada e não pode ser cancelada");
            }
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
            this.service.getGithubService().cancelWorkFlow(repository, bean.getWorkflow());
            bean.setConclusion(GithubActionConclusionEnum.cancelling);
            bean.setUser(this.getLoggedUser());
            this.service.getRepository().save(bean);
            return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/rerun-failed/{id}")
    public ResponseEntity<AtualizacaoVersaoBean> rerunFailed(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        this.target = produto;
        AtualizacaoVersaoBean bean = this.service.getRepository().findById(id).get();
        if (bean.getStatus() != GithubActionStatusEnum.completed) {
            throw new Exception("A requisição ainda não foi processada. Espere o término e tente novamente");
        }
        if (bean.getConclusion() == GithubActionConclusionEnum.success) {
            throw new Exception("A requisição foi finalizada com sucesso. Não pode ser atualizada");
        }
        for (AtualizacaoVersaoModuloBean mod: bean.getModules()) {
            if (mod.isEnabled() && !mod.getEnvironmentModule().getProjectModule().isFramework()) {

                AmbienteModuloBean modAmbiente = mod.getEnvironmentModule();
                AmbienteModuloConfigDTO config = modAmbiente.getConfig();
                //Apenas para verificar se o runner está no ar
                String runnerIdentifier = this.service.getGithubIdentifier(config.getRunnerId().longValue(), modAmbiente.getProjectModule().getTitle());
            }
        }
        String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
        this.service.getGithubService().rerunFailedJobsWorkFlow(repository, bean.getWorkflow());
        bean.setRequestDate(Calendar.getInstance());
        bean.setConclusion(null);
        bean.setConclusionDate(null);
        bean.setStatus(GithubActionStatusEnum.queued);
        bean.setUser(this.getLoggedUser());
        this.service.getRepository().save(bean);

        return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
    }

    @GetMapping("/{produto}/logs/{id}")
    public ResponseEntity<AtualizacaoVersaoBean> getLogs(@PathVariable("produto") String produto, @PathVariable("id") Long id) {
        this.target = produto;
        return ResponseEntity.ok(this.service.getRepository().findById(id).orElse(null));
    }

    @GetMapping("/{produto}/branches")
    public ResponseEntity<AvailableVersionDTO> getBranches(@PathVariable("produto") String produto) throws Exception {
        try {
            this.target = produto;
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            return ResponseEntity.ok(this.service.getGithubService().getBranches(this.projectRepository.getRepositoryFromIdentifier(this.target)));
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/initial-data")
    public ResponseEntity<LinkedHashMap<String, Object>> getInitialData(@PathVariable("produto") String produto) throws Exception {
        this.deleteOldResults();
        this.target = produto;
        long pendings = this.service.getRepository().countByStatusNotAndEnvironmentProjectIdentifierAndWorkflowIsNotNull(GithubActionStatusEnum.completed, produto);
        if (pendings > 0) {
            this.searchPendingVersion();
        }
        LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
        resp.put("environments", this.service.getAmbienteRepository().findAllByProjectIdentifierOrderByIdentifierAsc(produto));
        resp.put("versions", this.service.getVersaoRepository().findAllByProjectIdentifierOrderByMajorDescMinorDescPatchDescVersionTypeAscBuildDesc(produto));
        resp.put("history", this.service.getRepository().findAllByConclusionAndEnvironmentProjectIdentifierOrderByConclusionDateDesc(GithubActionConclusionEnum.success, produto));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{produto}/link/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getLink(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        try {
            this.target = produto;
            if (!this.service.getGithubService().initialize()) {
                throw new Exception("Configuração github não foi feita");
            }
            String repository = this.projectRepository.getRepositoryFromIdentifier(this.target);
            LinkedHashMap<String, Object> resp = new LinkedHashMap();
            resp.put("resp", this.service.getGithubService().getLinkWorkflow(repository, id));
            return ResponseEntity.ok(resp);
        }
        finally {
            this.service.getGithubService().dispose();
        }
    }

    @GetMapping("/{produto}/errors/{id}")
    public ResponseEntity<LinkedHashMap<String, Object>> getErrors(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        this.target = produto;
        AtualizacaoVersaoBean bean = this.service.getRepository().findById(id).get();
        LinkedHashMap<String, Object> resp = new LinkedHashMap();
        resp.put("resp", bean.getError());
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER', 'ROLE_HYPER')")
    @DeleteMapping("/{produto}/{id}")
    public ResponseEntity<Void> delete(@PathVariable("produto") String produto, @PathVariable("id") Long id) throws Exception {
        this.target = produto;
        AtualizacaoVersaoBean bean = this.service.getRepository().findById(id).get();
        if (bean.getStatus() == GithubActionStatusEnum.completed || bean.getWorkflow() != null) {
            if (bean.getConclusion() == GithubActionConclusionEnum.success) {
                throw new Exception("Apenas requisições não finalizadas ou com falhas e que não tenham sido disparadas no Github é que podem ser apagadas");
            }
        }
        if (bean.getStatus() == GithubActionStatusEnum.scheduled) {
            AtualizacaoVersaoService.cancelSchedule(bean.getId());
        }
        else { //Usuarios ADMIN soh podem apagar tarefas agendadas
            UserBean user = this.getLoggedUser();
            if (user.getProfile() == ProfileEnum.ROLE_ADMIN) {
                throw new Exception("Apenas requisições agendadas podem ser apagadas pelo seu usuário");
            }
        }

        this.service.getRepository().deleteById(id);
        return ResponseEntity.ok(null);
    }

    private UserBean getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsSecurity user = (UserDetailsSecurity) auth.getPrincipal();
        return this.userRepository.findById(user.getId()).get();
    }


    private void searchPendingVersion() {
        if (AtualizacaoVersaoService.getSemaphore(this.target).getQueueLength() == 0) {
            this.service.searchPending(this.target, null, null);
        }
    }

    private void deleteOldResults() {
        if (AtualizacaoVersaoService.getDeleteSemaphore().getQueueLength() == 0) {
            this.service.deleteOldResults();
        }
    }
}
