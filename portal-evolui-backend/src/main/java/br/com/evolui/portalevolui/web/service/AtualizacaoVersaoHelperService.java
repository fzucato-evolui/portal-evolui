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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final class DispatchPreparation {
        final String repository;
        final GithubAtualizacaoVersaoDTO dto;

        DispatchPreparation(String repository, GithubAtualizacaoVersaoDTO dto) {
            this.repository = repository;
            this.dto = dto;
        }
    }

    /**
     * Usado apenas pelo agendador (thread em background, sem OSIV): carrega o bean e monta o
     * payload de dispatch numa transação curta e só-leitura (nada é persistido aqui — o bean só é
     * salvo depois que o workflow é confirmado), solta a transação, dispara e espera sem nenhuma
     * conexão aberta, e só então grava o resultado numa segunda transação curta.
     */
    public Map.Entry<AtualizacaoVersaoBean, Throwable> generateVersion(Long id) throws Exception {
        TransactionTemplate readTx = new TransactionTemplate(this.transactionManager);
        readTx.setReadOnly(true);

        AtualizacaoVersaoBean bean;
        DispatchPreparation prepared;
        try {
            Map.Entry<AtualizacaoVersaoBean, DispatchPreparation> loaded = readTx.execute(status -> {
                AtualizacaoVersaoBean b = this.getRepository().findById(id).get();
                try {
                    return new AbstractMap.SimpleEntry<>(b, this.buildDispatchPreparation(b));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            bean = loaded.getKey();
            prepared = loaded.getValue();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            AtualizacaoVersaoBean bean2 = this.getRepository().findById(id).orElse(null);
            if (bean2 == null) {
                throw (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
            }
            return new AbstractMap.SimpleEntry<>(bean2, cause);
        }

        try {
            GithubWorkflowDTO workflowDTO = this.getGithubService().callUpdater(prepared.repository, prepared.dto);
            bean.setWorkflow(workflowDTO.getId());
            bean.setStatus(workflowDTO.getStatus());
            bean.setConclusion(workflowDTO.getConclusion());

            TransactionTemplate writeTx = new TransactionTemplate(this.transactionManager);
            writeTx.execute(status -> {
                this.getRepository().save(bean);
                return null;
            });
            return new AbstractMap.SimpleEntry<>(bean, null);
        } catch (Throwable e) {
            return new AbstractMap.SimpleEntry<>(bean, e);
        }
    }

    /**
     * Usado pelo fluxo síncrono via HTTP, onde o OSIV mantém a conexão da requisição aberta durante
     * todo o dispatch/espera independentemente de qualquer transação explícita aqui — encurtar uma
     * transação neste caminho não reduziria a retenção de conexão.
     */
    public AtualizacaoVersaoBean generateVersion(AtualizacaoVersaoBean bean) throws Exception {
        DispatchPreparation prepared = this.buildDispatchPreparation(bean);
        GithubWorkflowDTO workflowDTO = this.getGithubService().callUpdater(prepared.repository, prepared.dto);
        bean.setWorkflow(workflowDTO.getId());
        bean.setStatus(workflowDTO.getStatus());
        bean.setConclusion(workflowDTO.getConclusion());
        return bean;
    }

    private DispatchPreparation buildDispatchPreparation(AtualizacaoVersaoBean bean) throws Exception {
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
            boolean isMain = mod.getEnvironmentModule().getProjectModule().isMain();
            // O runner do módulo principal é usado no "runs-on" do action mesmo quando o próprio
            // módulo principal não está habilitado nesta atualização (só um submódulo está) —
            // por isso não pode cair no continue abaixo, senão o runnerIdentifier nunca é resolvido.
            if (!mod.isEnabled() && !isMain) {
                continue;
            }
            AmbienteModuloBean modAmbiente = ambiente.getModules().stream().filter(x -> x.getId().equals(mod.getEnvironmentModule().getId())).findFirst().orElse(null);
            if (modAmbiente == null) {
                mod.setEnabled(false);
                continue;
            }
            AmbienteModuloConfigDTO config = modAmbiente.getConfig();

            // Módulo desabilitado no ambiente não participa da atualização
            if (!Boolean.TRUE.equals(config.getEnabled())) {
                mod.setEnabled(false);
                continue;
            }

            // Runner do módulo principal sempre é usado no action
            if (isMain || (mod.isEnabled() && !mod.getEnvironmentModule().getProjectModule().isFramework())) {
                if (config.getRunnerId() == null) {
                    throw new Exception(String.format("Módulo %s não possui runner configurado no ambiente", modAmbiente.getProjectModule().getTitle()));
                }
                String runnerIdentifier = this.getGithubIdentifier(config.getRunnerId().longValue(), modAmbiente.getProjectModule().getTitle());
                config.setRunnerIdentifier(runnerIdentifier);
                modAmbiente.setConfig(config);
            }
        }

        boolean anyModuleEnabled = bean.getModules().stream()
                .anyMatch(m -> m.isEnabled()
                        && (m.getEnvironmentModule().getProjectModule().isMain()
                            || !m.getEnvironmentModule().getProjectModule().isFramework()));
        if (!anyModuleEnabled) {
            throw new Exception("Nenhum módulo está habilitado para esta atualização de versão");
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
        return new DispatchPreparation(ambiente.getProject().getRepository(), dto);
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
