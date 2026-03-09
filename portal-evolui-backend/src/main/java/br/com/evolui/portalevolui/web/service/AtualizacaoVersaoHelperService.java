package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.*;
import br.com.evolui.portalevolui.web.beans.dto.AmbienteModuloConfigDTO;
import br.com.evolui.portalevolui.web.beans.enums.CompileTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionStatusEnum;
import br.com.evolui.portalevolui.web.repository.ambiente.AmbienteRepository;
import br.com.evolui.portalevolui.web.repository.atualizacao_versao.AtualizacaoVersaoRepository;
import br.com.evolui.portalevolui.web.repository.versao.VersaoRepository;
import br.com.evolui.portalevolui.web.rest.dto.enums.GithubRunnerLabelTypeEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubAtualizacaoVersaoDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerLabelDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubWorkflowDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class AtualizacaoVersaoHelperService {
    @Value("${evolui.base-url}")
    private String baseUrl;
    @Value("${server.port}")
    private Integer port;
    @Autowired
    private
    GithubVersionService githubService;
    @Autowired
    private
    AtualizacaoVersaoRepository repository;
    @Autowired
    private
    AmbienteRepository ambienteRepository;
    @Autowired
    private
    VersaoRepository versaoRepository;

    @Transactional(propagation=REQUIRES_NEW)
    public Map.Entry<AtualizacaoVersaoBean, Throwable> generateVersion(Long id) throws Exception {
        AtualizacaoVersaoBean bean = this.getRepository().findById(id).get();
        try {
            return new AbstractMap.SimpleEntry<>(this.generateVersion(bean), null);
        } catch (Throwable e) {
            return new AbstractMap.SimpleEntry<>(bean, e);
        }
    }

    public AtualizacaoVersaoBean generateVersion(AtualizacaoVersaoBean bean) throws Exception {
        AmbienteBean ambiente = bean.getEnvironment();
        if (bean.getId() != null && bean.getId() > 0) {
            if (this.getRepository()
                    .countByStatusNotAndEnvironmentIdAndIdNot
                            (GithubActionStatusEnum.completed, ambiente.getId(), bean.getId()) > 0) {
                throw new Exception("Já existe uma versão sendo atualizada");
            }
        } else {
            if (ambiente.getBusy()) {
                throw new Exception("Já existe uma versão sendo atualizada");
            }
        }
        for (AtualizacaoVersaoModuloBean mod: bean.getModules()) {
            // Runner do módulo principal sempre é usado no action
            if (mod.getEnvironmentModule().getProjectModule().isMain() || (mod.isEnabled() && !mod.getEnvironmentModule().getProjectModule().isFramework())) {

                AmbienteModuloBean modAmbiente = ambiente.getModules().stream().filter(x -> x.getId().equals(mod.getEnvironmentModule().getId())).findFirst().get();
                AmbienteModuloConfigDTO config = modAmbiente.getConfig();
                String runnerIdentifier = this.getGithubIdentifier(config.getRunnerId().longValue(), modAmbiente.getProjectModule().getTitle());
                config.setRunnerIdentifier(runnerIdentifier);
                modAmbiente.setConfig(config);
            }
        }
        List<VersaoBean> versions = new ArrayList<>();
        if (bean.compareTo(ambiente) == 0) {
            Optional<VersaoBean> ov = this.getVersaoRepository().findByTagAndProjectIdentifier(ambiente.getTag(), ambiente.getProject().getIdentifier());
            if (ov.isPresent()) {
                versions.add(ov.get());
            }
        }
        else {
            versions = this.getVersaoRepository().findAllByProjectIdentifier(ambiente.getProject().getIdentifier());
            for (int i = 0; i < versions.size();) {
                VersaoBean v = versions.get(i);
                if (CompileTypeEnum.isTransitoryType(v.getVersionType()) && bean.compareTo(v) != 0) {
                    versions.remove(i);
                }
                else if (v.compareTo(bean) > 0 || v.compareTo(ambiente) <= 0) {
                    versions.remove(i);
                } else {
                    i++;
                }
            }
        }
        
        if (versions.isEmpty()) {
            throw  new Exception("Nenhuma versão encontrada para atualização");
        }
        Collections.sort(versions);
        bean.setRequestDate(Calendar.getInstance());
        bean.setTags(versions.stream().map(x -> x.getTag()).collect(Collectors.toList()));
        String webhook = String.format("%s:%s/api/public/github/webhook-atualizacao-versao/%s/%s", baseUrl, port, ambiente.getProject().getIdentifier(), bean.getHashToken());
        GithubAtualizacaoVersaoDTO dto = GithubAtualizacaoVersaoDTO.fromBean(bean, versions, webhook);
        System.out.println(new ObjectMapper().writeValueAsString(dto));
        bean.setStatus(GithubActionStatusEnum.queued);
        GithubWorkflowDTO workflowDTO = this.getGithubService().callUpdater(ambiente.getProject().getRepository(), dto);
        bean.setWorkflow(workflowDTO.getId());
        bean.setStatus(workflowDTO.getStatus());
        bean.setConclusion(workflowDTO.getConclusion());
        return  bean;
    }

    public String getGithubIdentifier(Long id, String modulo) throws Exception {
        GithubRunnerDTO runner = this.getGithubService().getRunner(id);
        if (runner == null) {
            throw new Exception(String.format("Runner do módulo %s não foi encontrado no Github", modulo));
        } else if (!runner.getStatus().equals("online")) {
            throw new Exception(String.format("Runner do módulo %s não está online. Se a máquina foi inicializada recentemente, aguarde que o serviço seja iniciado", modulo));
        }

        String identifier = runner.getName();
        if (runner.getLabels() != null && !runner.getLabels().isEmpty()) {
            for (GithubRunnerLabelDTO label : runner.getLabels()) {
                if (label.getType() == GithubRunnerLabelTypeEnum.CUSTOM) {
                    identifier = label.getName();
                }
            }
        }
        if (runner.isBusy()) { //Pode fazer simultâneo desde que cada ambiente tenha a sua própria pasta
            //throw new Exception(String.format("Runner %s já está executando outra atualização.", identifier));
        }
        return identifier;
    }

    public GithubVersionService getGithubService() {
        return githubService;
    }

    public AtualizacaoVersaoRepository getRepository() {
        return repository;
    }

    public AmbienteRepository getAmbienteRepository() {
        return ambienteRepository;
    }

    public VersaoRepository getVersaoRepository() {
        return versaoRepository;
    }
}
