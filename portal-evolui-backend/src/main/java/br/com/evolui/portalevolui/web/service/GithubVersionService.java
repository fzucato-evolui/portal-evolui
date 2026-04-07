package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.VersaoBuildBaseBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.config.GithubConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.enums.GithubWorkflowType;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import br.com.evolui.portalevolui.web.rest.dto.github.runner.ActionsRunnerLatestDownloadDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.AvailableVersionDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.BranchDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import br.com.evolui.portalevolui.web.util.FunctionsUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class GithubVersionService implements ISystemConfigService {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    @Autowired
    private SystemConfigRepository configRepository;
    
    private GithubConfigDTO config;
    
    private SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public GithubWorkflowDTO callBuilder(String repository, GithubGeracaoVersaoDTO dto) throws Exception {
        String url = this.getUrlBuilder(repository);
        RestClientService rest = RestClientService.using(url, this.getConfig().getToken());

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("input", dto);

        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("event_type", GithubWorkflowType.builder.value());
        body.put("client_payload", input);
        rest.doRequest(HttpMethod.POST, body);
        Future<GithubWorkflowDTO> f = null;
        try {
            Calendar c = Calendar.getInstance();
            //Para não correr o risco de alguma diferença de horários
            c.add(Calendar.MINUTE, -10);
            f = this.getRecentDispatch(repository, dto.getHashToken(), c);
            GithubWorkflowDTO resp = f.get(60, TimeUnit.SECONDS);
            return resp;
        } catch (Exception ex) {
            throw  ex;
        } finally {
            try {
                f.cancel(true);
            } catch (Exception e){}

        }

    }

    public GithubWorkflowDTO callUpdater(String repository, GithubAtualizacaoVersaoDTO dto) throws Exception {
        String url = this.getUrlBuilder(repository);
        RestClientService rest = RestClientService.using(url, this.getConfig().getToken());

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("input", dto);

        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("event_type", GithubWorkflowType.updater.value());
        body.put("client_payload", input);
        rest.doRequest(HttpMethod.POST, body);
        Future<GithubWorkflowDTO> f = null;
        try {
            Calendar c = Calendar.getInstance();
            //Para não correr o risco de alguma diferença de horários
            c.add(Calendar.MINUTE, -10);
            f = this.getRecentDispatch(repository, dto.getHashToken(), c);
            GithubWorkflowDTO resp = f.get(60, TimeUnit.SECONDS);
            return resp;
        } catch (Exception ex) {
            throw  ex;
        } finally {
            try {
                f.cancel(true);
            } catch (Exception e){}

        }

    }

    public GithubWorkflowDTO callCICD(String repository, GithubCICDDTO dto) throws Exception {
        String url = this.getUrlBuilder(repository);
        RestClientService rest = RestClientService.using(url, this.getConfig().getToken());

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("input", dto);

        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("event_type", GithubWorkflowType.tester.value());
        body.put("client_payload", input);
        rest.doRequest(HttpMethod.POST, body);
        Future<GithubWorkflowDTO> f = null;
        try {
            Calendar c = Calendar.getInstance();
            //Para não correr o risco de alguma diferença de horários
            c.add(Calendar.MINUTE, -10);
            f = this.getRecentDispatch(repository, dto.getHashToken(), c);
            GithubWorkflowDTO resp = f.get(60, TimeUnit.SECONDS);
            return resp;
        } catch (Exception ex) {
            throw  ex;
        } finally {
            try {
                f.cancel(true);
            } catch (Exception e){}

        }

    }

    private Future<GithubWorkflowDTO> getRecentDispatch(final String repository, final String identifier, final Calendar referenceDate) throws Exception {
        String url = this.getUrlBuilderRunning(repository, referenceDate);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Callable<GithubWorkflowDTO> callable = () -> {
            while (true) {

                GithubWorkflowListDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubWorkflowListDTO.class);
                if (resp.getTotal_count() > 0) {
                    for (GithubWorkflowDTO dto : resp.getWorkflow_runs()) {
                        if (dto.getUpdated_at().compareTo(referenceDate) >= 0 &&
                            dto.getActor().getLogin().equals(this.getConfig().getUser())) {
                            GithubJobListDTO jobs = this.getWorkflowJobs(repository, dto.getId());
                            if (jobs.getTotal_count() > 0) {
                                if (jobs.getJobs().stream().filter(x -> x.getName().equals(identifier)).findFirst().isPresent()) {
                                    return dto;
                                }
                            }

                        }
                    }

                }
                Thread.sleep(500);

            }
        };
        FutureTask<GithubWorkflowDTO> f = new FutureTask<GithubWorkflowDTO>(callable);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(f);
        return f;
    }

    public GithubWorkflowDTO getWorkFlow(String repository, Long runId) throws Exception {
        String url = this.getUrlWorkflow(repository, runId);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubWorkflowDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubWorkflowDTO.class);
        return resp;
    }

    public void cancelWorkFlow(String repository, Long runId) throws Exception {
        String url = this.getUrlWorkflowCancel(repository, runId);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        rest.doRequest(HttpMethod.POST, null);
    }

    public void rerunFailedJobsWorkFlow(String repository, Long runId) throws Exception {
        String url = this.getUrlWorkflowRerunFailedJob(repository, runId);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        rest.doRequest(HttpMethod.POST, null);
    }

    public GithubJobListDTO getWorkflowJobs(String repository, Long runId) throws Exception {
        String url = this.getUrlWorkflowJobs(repository, runId);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubJobListDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubJobListDTO.class);
        return resp;
    }

    public GithubJobDTO getJob(String repository, Long id) throws Exception {
        String url = this.getUrlJob(repository, id);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubJobDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubJobDTO.class);
        return resp;
    }

    public String getJobLogs(String repository, Long id) throws Exception {
        String url = this.getUrlJobLogs(repository, id);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken(), true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return rest.doRequest(HttpMethod.GET, null);
    }

    public AvailableVersionDTO getBranches(String repository) throws GitAPIException {
        AvailableVersionDTO a = new AvailableVersionDTO();
        UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(getConfig().getUser(), getConfig().getToken());
        String url = String.format("https://github.com/%s/%s.git", this.getConfig().getOwner(), repository);
        Collection<Ref> remoteRefs = Git.lsRemoteRepository()
                .setCredentialsProvider(cp)
                .setRemote(url)
                .setTags(true)
                .setHeads(true)
                .call();
        if (remoteRefs != null && !remoteRefs.isEmpty()) {
            List<Ref> versions = remoteRefs.stream().filter(x -> !x.getName().toLowerCase().contains("master"))
                    .sorted(Comparator.comparing(Ref::getName, (s1, s2) -> {
                        String version1 = s1.replace("refs/heads/", "").replace("refs/tags/", "");
                        String version2 = s2.replace("refs/heads/", "").replace("refs/tags/", "");
                        return new VersaoBuildBaseBean(version2).compareTo(new VersaoBuildBaseBean(version1));
                    })).collect(Collectors.toList());
            List<BranchDTO> branches = versions.stream().filter(x -> !x.getName().contains("tags")).map(x -> {
                BranchDTO dto = new BranchDTO();
                dto.setVersion(x.getName().replace("refs/heads/", ""));
                return dto;
            }).collect(Collectors.toList());

            for (BranchDTO b : branches) {
                b.setLastTag(versions.stream().filter(x -> x.getName().contains(b.getVersion()) && !x.getName().contains("heads")).findFirst()
                        .map(x -> x.getName().replace("refs/tags/", "")).orElse(null));
            }
            a.setBranches(branches);
        }
        return a;

    }

    public List<GithubBranchDTO> getAllBranches(String repository) throws Exception {
        String url = this.getUrlBranches(repository);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<GithubBranchDTO> resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null),  new TypeReference<List<GithubBranchDTO>>(){});
        return resp;

    }

    public GithubBranchDTO getBranch(String repository, String branch, String relativePath) throws Exception {
        if (!StringUtils.hasText(relativePath)) {
            String url = this.getUrlBranch(repository, branch);
            //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
            RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            GithubBranchDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubBranchDTO.class);
            return resp;
        }
        else {
            String url = this.getUrlBranchFolderLastCommit(repository, branch, relativePath);
            //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
            RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            GithubBranchDTO.Commit commit = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubBranchDTO.Commit.class);
            GithubBranchDTO resp = new GithubBranchDTO();
            resp.setCommit(commit);
            resp.setName(branch);
            return resp;
        }

    }

    public void deleteBranch(String repository, String branch) throws Exception {
        String url = this.getUrlDeleteBranches(repository, branch);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        rest.doRequest(HttpMethod.DELETE, null);
    }

    public List<GithubTagDTO> getAllTags(String repository) throws Exception {
        String url = this.getUrlTags(repository);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<GithubTagDTO> resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null),  new TypeReference<List<GithubTagDTO>>(){});
        return resp;

    }

    public BranchesAndTagsDetailDTO getBranchesAndTagsDetailed(String repository) throws Exception {
        List<GithubBranchDTO> branches = this.getAllBranches(repository);
        List<GithubTagDTO> tags = this.getAllTags(repository);

        // Coletar todos os SHAs únicos que precisamos resolver
        Set<String> neededShas = new HashSet<>();
        for (GithubBranchDTO b : branches) {
            if (b.getCommit() != null && b.getCommit().getSha() != null) {
                neededShas.add(b.getCommit().getSha());
            }
        }
        for (GithubTagDTO t : tags) {
            if (t.getCommit() != null && t.getCommit().getSha() != null) {
                neededShas.add(t.getCommit().getSha());
            }
        }

        Map<String, GithubCommitHistoryDTO> commitCache = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Buscar detalhes em lote: para cada branch, buscar lista de commits recentes (100 por chamada).
        // Cada chamada retorna detalhes completos de até 100 commits, cobrindo a maioria dos SHAs das tags.
        // Com ~10 branches, são ~10 chamadas em vez de ~96 individuais.
        for (GithubBranchDTO branch : branches) {
            if (neededShas.isEmpty()) break;
            try {
                String url = this.getUrlCommitsByBranch(repository, branch.getName());
                RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
                List<GithubCommitHistoryDTO> commits = mapper.readValue(
                        rest.doRequest(HttpMethod.GET, null),
                        new TypeReference<List<GithubCommitHistoryDTO>>(){}
                );
                for (GithubCommitHistoryDTO commit : commits) {
                    if (neededShas.remove(commit.getSha())) {
                        commitCache.put(commit.getSha(), commit);
                    }
                }
            } catch (Exception e) {
                // Se falhar para uma branch, continuar com as demais
            }
        }

        // Montar lista de branches detalhadas
        List<GitRefDetailDTO> detailedBranches = new ArrayList<>();
        for (GithubBranchDTO b : branches) {
            String sha = b.getCommit() != null ? b.getCommit().getSha() : null;
            GitRefDetailDTO detail = buildGitRefDetail(b.getName(), sha, commitCache);
            detailedBranches.add(detail);
        }

        // Montar lista de tags detalhadas (somente tags cujo commit foi encontrado)
        List<GitRefDetailDTO> detailedTags = new ArrayList<>();
        for (GithubTagDTO t : tags) {
            String sha = t.getCommit() != null ? t.getCommit().getSha() : null;
            if (sha != null && !commitCache.containsKey(sha)) {
                continue;
            }
            GitRefDetailDTO detail = buildGitRefDetail(t.getName(), sha, commitCache);
            detailedTags.add(detail);
        }

        return new BranchesAndTagsDetailDTO(detailedBranches, detailedTags);
    }

    private GitRefDetailDTO buildGitRefDetail(String name, String sha, Map<String, GithubCommitHistoryDTO> commitCache) {
        GitRefDetailDTO detail = new GitRefDetailDTO();
        detail.setName(name);
        detail.setSha(sha);

        if (sha != null && commitCache.containsKey(sha)) {
            GithubCommitHistoryDTO commitDetail = commitCache.get(sha);
            if (commitDetail.getCommit() != null) {
                detail.setMessage(commitDetail.getCommit().getMessage());
                if (commitDetail.getCommit().getAuthor() != null) {
                    detail.setAuthor(commitDetail.getCommit().getAuthor().getName());
                    detail.setDate(commitDetail.getCommit().getAuthor().getDate());
                }
            }
            // Fallback para login do autor do GitHub se não tiver nome no commit
            if (detail.getAuthor() == null && commitDetail.getAuthor() != null) {
                detail.setAuthor(commitDetail.getAuthor().getLogin());
            }
        }

        return detail;
    }

    public void deleteTag(String repository, String tag) throws Exception {
        String url = this.getUrlDeleteTags(repository, tag);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        rest.doRequest(HttpMethod.DELETE, null);
    }

    public String extractResultFromLogs(String logs) {
        int startIndex = logs.lastIndexOf("#JSON_RESULT#=") + "#JSON_RESULT#=".length();
        int endIndex = logs.lastIndexOf("#END_JSON_RESULT#");
        return logs.substring(startIndex, endIndex);
    }

    public GithubRunnerListDTO getRunners() throws Exception {
        String url = this.getUrlRunner(null);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubRunnerListDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubRunnerListDTO.class);
        return resp;
    }

    public GithubRunnerDTO getRunner(Long id) throws Exception {
        String url = this.getUrlRunner(id);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubRunnerDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubRunnerDTO.class);
        return resp;
    }

    /**
     * Token de registro de self-hosted runner na organização configurada ({@link GithubConfigDTO#getOwner()}).
     */
    public GithubRunnerRegistrationTokenDTO createOrganizationRunnerRegistrationToken() throws Exception {
        String url = String.format("%s/orgs/%s/actions/runners/registration-token",
                GITHUB_API_BASE_URL, getConfig().getOwner());
        RestClientService rest = RestClientService.using(url, true, getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = rest.doRequest(HttpMethod.POST, new LinkedHashMap<>());
        return mapper.readValue(json, GithubRunnerRegistrationTokenDTO.class);
    }

    /**
     * Remove o runner apenas do lado GitHub (org). O agente na máquina pode continuar até ser desinstalado localmente.
     */
    public void deleteOrganizationRunner(long runnerId) throws Exception {
        String url = getUrlRunner(runnerId);
        RestClientService rest = RestClientService.using(url, true, getConfig().getToken());
        rest.doRequest(HttpMethod.DELETE, null);
    }

    /**
     * Token de curta duração para {@code config.sh|config.cmd remove --token ...} na pasta do runner.
     */
    public GithubRunnerRegistrationTokenDTO createOrganizationRunnerRemoveToken() throws Exception {
        String url = String.format("%s/orgs/%s/actions/runners/remove-token",
                GITHUB_API_BASE_URL, getConfig().getOwner());
        RestClientService rest = RestClientService.using(url, true, getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = rest.doRequest(HttpMethod.POST, new LinkedHashMap<>());
        return mapper.readValue(json, GithubRunnerRegistrationTokenDTO.class);
    }

    public boolean isRunnerNameAvailable(String runnerName) throws Exception {
        if (!StringUtils.hasText(runnerName)) {
            return false;
        }
        GithubRunnerListDTO list = getRunners();
        if (list == null || list.getRunners() == null || list.getRunners().isEmpty()) {
            return true;
        }
        return list.getRunners().stream().noneMatch(r -> runnerName.equalsIgnoreCase(r.getName()));
    }

    /**
     * Último release público de {@code actions/runner} (zip win-x64 ou tar.gz/linux-x64).
     *
     * @param osFamily texto contendo {@code win} ou {@code linux}
     */
    public ActionsRunnerLatestDownloadDTO resolveLatestActionsRunnerDownload(String osFamily) throws Exception {
        String url = GITHUB_API_BASE_URL + "/repos/actions/runner/releases/latest";
        RestClientService rest = RestClientService.using(url, true, getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubReleaseLatestDTO release = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubReleaseLatestDTO.class);
        if (release == null || release.getAssets() == null || release.getAssets().isEmpty()) {
            throw new Exception("Resposta inválida da API de releases do actions/runner");
        }
        String version = release.getTagName() != null
                ? release.getTagName().replaceFirst("^[vV]", "")
                : "";
        String os = osFamily != null ? osFamily.trim().toLowerCase(Locale.ROOT) : "";
        boolean win = os.contains("win");
        boolean linux = os.contains("linux");
        if (!win && !linux) {
            throw new Exception("Parâmetro os deve indicar windows ou linux");
        }
        GithubReleaseLatestDTO.Asset chosen = null;
        if (win) {
            for (GithubReleaseLatestDTO.Asset asset : release.getAssets()) {
                if (asset.getName() == null || asset.getBrowserDownloadUrl() == null) {
                    continue;
                }
                String name = asset.getName().toLowerCase(Locale.ROOT);
                if (name.contains("win-x64") && name.endsWith(".zip")) {
                    chosen = asset;
                    break;
                }
            }
        } else {
            GithubReleaseLatestDTO.Asset tarGz = null;
            GithubReleaseLatestDTO.Asset zip = null;
            for (GithubReleaseLatestDTO.Asset asset : release.getAssets()) {
                if (asset.getName() == null || asset.getBrowserDownloadUrl() == null) {
                    continue;
                }
                String name = asset.getName().toLowerCase(Locale.ROOT);
                if (name.contains("linux-x64")) {
                    if (name.endsWith(".tar.gz")) {
                        tarGz = asset;
                    } else if (name.endsWith(".zip")) {
                        zip = asset;
                    }
                }
            }
            chosen = tarGz != null ? tarGz : zip;
        }
        if (chosen == null) {
            throw new Exception("Não foi encontrado pacote actions-runner para o SO informado");
        }
        ActionsRunnerLatestDownloadDTO dto = new ActionsRunnerLatestDownloadDTO();
        dto.setVersion(version);
        dto.setAssetName(chosen.getName());
        dto.setDownloadUrl(chosen.getBrowserDownloadUrl());
        return dto;
    }

    public List<GithubMemberDTO> getMembers() throws Exception {
        String url = this.getUrlMembers();
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<GithubMemberDTO> resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), new TypeReference<>() {
        });
        List<GithubMemberDTO> detailedResp = new ArrayList<>();
        for (GithubMemberDTO m : resp) {
            GithubMemberDTO detailedM = mapper.readValue(RestClientService.using(m.getUrl(), true, this.getConfig().getToken()).doRequest(HttpMethod.GET, null), GithubMemberDTO.class);
            detailedResp.add(detailedM);
        }
        return detailedResp;
    }

    public List<GithubDetailedRepositoryDTO> getRepositories() throws Exception {
        List<GithubDetailedRepositoryDTO> all = new ArrayList<>();
        int page = 1;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        while (true) {
            String url = this.getUrlRepositories(page);
            //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
            RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
            List<GithubDetailedRepositoryDTO> resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), new TypeReference<>() {
            });
            if (resp.isEmpty()) {
                break;
            }
            all.addAll(resp);
            page++;
        }

        return all;
    }

    public List<GithubCommitHistoryDTO> getCommitHistory(String repository, String branch, Calendar since, Calendar until) throws Exception {

        List<GithubCommitHistoryDTO> respAll = new ArrayList<>();
        Integer page = 0;
        while (true) {
            String url = this.getUrlCommits(repository, branch, since, until, page);
            //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
            RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<GithubCommitHistoryDTO> resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), new TypeReference<>() {
            });
            respAll.addAll(resp);
            if (resp.size() < 100) {
                break;
            }
            else {
                page++;
            }
        }
        return respAll;
    }

    public List<GithubDetailedRepositoryDTO> getWorflowRepositories() throws Exception {
        Set<String> topics = Set.of("workflow", "pipeline");
        List<GithubDetailedRepositoryDTO> all = new ArrayList<>();
        int page = 1;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String queryTopics = String.format(
                topics.stream().map(q -> "topic:" + q).collect(Collectors.joining("+")) +
                "+org:%s", this.getConfig().getOwner());
        while (true) {

            String url =  String.format("https://api.github.com/search/repositories?q=%s&per_page=100&page=%d",
                    queryTopics, page);
            //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
            RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
            GithubSearchRepositoryDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubSearchRepositoryDTO.class);
            all.addAll(resp.getItems());
            if (resp.getItems().size() < 100) {
                break;
            }
            page++;
        }

        return all;
    }

    public ProjectBean getProjectWorkflowRepositoryStructure(String repository) throws Exception {
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
                this.getConfig().getOwner(), repository, "master", ".spec/structure.json");
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = rest.doRequest(HttpMethod.GET, null);
        return mapper.readValue(json, ProjectBean.class);
    }

    public String getFileContent(String repository, String branch, String filePath) throws Exception {
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
                this.getConfig().getOwner(), repository, branch, filePath);
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = rest.doRequest(HttpMethod.GET, null);
        return content;
    }

    public GithubBranchCreateResponseDTO createBranch(String repository, String branch, String hashCommit) throws Exception {
        String url = String.format("%s/repos/%s/%s/git/refs",
                GITHUB_API_BASE_URL, this.getConfig().getOwner(), repository);
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubBranchCreateRequestDTO request = new GithubBranchCreateRequestDTO();
        request.setBranchName(branch);
        request.setSha(hashCommit);
        GithubBranchCreateResponseDTO response = mapper.readValue(rest.doRequest(HttpMethod.POST, mapper.writeValueAsString(request)), GithubBranchCreateResponseDTO.class);
        return response;
    }

    @Async
    public void createBranchesAsync(String branchName, Map<String, String> repoHashCommit) {
        repoHashCommit.entrySet().parallelStream().forEach(entry -> {
            String repo = entry.getKey();
            String commit = entry.getValue();

            try {
                this.createBranch(repo, branchName, commit);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public boolean initialize(Object... param) {
        return this.getConfig() != null;
    }

    @Override
    public void dispose() {
        this.config = null;
    }

    @Override
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.GITHUB).orElse(null);
    }

    public GithubDiffCommitListDTO getDiffs (String repository, String startHash, String endHash) throws Exception {
        String url = this.getUrlDiffs(repository, startHash, endHash);
        //Tem que ser a URL Pura, pois ele acaba formatando a consulta e não funciona
        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GithubDiffCommitListDTO resp = mapper.readValue(rest.doRequest(HttpMethod.GET, null), GithubDiffCommitListDTO.class);
        return resp;
    }

    /**
     * Tipo de conteúdo para filtragem.
     */
    public enum ContentType {
        ALL,
        FILES_ONLY,
        DIRECTORIES_ONLY
    }

    public List<GithubContentDTO> listContentsAtPath(String repository, String path, ContentType contentType) throws Exception {
        GithubConfigDTO cfg = this.getConfig();
        String normalizedPath = path != null ? path.trim() : "";
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        String url;
        if (StringUtils.hasText(normalizedPath)) {
            url = String.format("%s/repos/%s/%s/contents/%s",
                    GITHUB_API_BASE_URL, cfg.getOwner(), repository, normalizedPath);
        } else {
            url = String.format("%s/repos/%s/%s/contents",
                    GITHUB_API_BASE_URL, cfg.getOwner(), repository);
        }

        RestClientService rest = RestClientService.using(url, true, this.getConfig().getToken());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<GithubContentDTO> allContents = mapper.readValue(rest.doRequest(HttpMethod.GET, null),new TypeReference<List<GithubContentDTO>>(){});
        // Filtra por tipo se necessário
        return switch (contentType) {
            case FILES_ONLY -> allContents.stream()
                    .filter(GithubContentDTO::isFile)
                    .collect(Collectors.toList());
            case DIRECTORIES_ONLY -> allContents.stream()
                    .filter(GithubContentDTO::isDirectory)
                    .collect(Collectors.toList());
            default -> allContents;
        };
    }

    public String getLinkWorkflow(String repository, Long id) {
        return String.format("https://github.com/%s/%s/actions/runs/%s",
                this.getConfig().getOwner(), repository, id);
    }

    public String getLinkCheckrun(String repository, Long id) {
        return String.format("https://github.com/%s/%s/runs/%s",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlBuilder(String repository) {
        return String.format("https://api.github.com/repos/%s/%s/dispatches",
                this.getConfig().getOwner(), repository);
    }

    private String getUrlBuilderRunning(String repository, Calendar filter) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs%s",
                this.getConfig().getOwner(), repository, filter != null ? "?created=%3E=" + this.formatDate.format(filter.getTime()) : "");
    }

    private String getUrlWorkflow(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs/%s",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlWorkflowCancel(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs/%s/cancel",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlWorkflowRerunFailedJob(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs/%s/rerun-failed-jobs",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlWorkflowLogs(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs/%s/logs",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlWorkflowJobs(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/runs/%s/jobs",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlRunner(Long id) {
        return String.format("https://api.github.com/orgs/%s/actions/runners%s",
                this.getConfig().getOwner(), id != null ? "/" + id : "");
    }

    private String getUrlJob(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/jobs/%s",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlJobLogs(String repository, Long id) {
        return String.format("https://api.github.com/repos/%s/%s/actions/jobs/%s/logs",
                this.getConfig().getOwner(), repository, id);
    }

    private String getUrlDiffs(String repository, String startHash, String endHash) {
        return String.format("https://api.github.com/repos/%s/%s/compare/%s...%s",
                this.getConfig().getOwner(), repository, startHash, endHash);
    }

    private String getUrlBranches(String repository) {
        return String.format("https://api.github.com/repos/%s/%s/branches?per_page=100",
                this.getConfig().getOwner(), repository);
    }

    private String getUrlBranch(String repository, String branch) {
        return String.format("https://api.github.com/repos/%s/%s/branches/%s",
                this.getConfig().getOwner(), repository, branch);
    }

    private String getUrlBranchFolderLastCommit(String repository, String branch, String folder) {
        return String.format("https://api.github.com/repos/%s/%s/commits?sha=%s&path=%s&per_page=1",
                this.getConfig().getOwner(), repository, branch, folder);
    }

    private String getUrlDeleteBranches(String repository, String branch) {
        return String.format("https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                this.getConfig().getOwner(), repository, branch);
    }

    private String getUrlDeleteTags(String repository, String tag) {
        return String.format("https://api.github.com/repos/%s/%s/git/refs/tags/%s",
                this.getConfig().getOwner(), repository, tag);
    }

    private String getUrlTags(String repository) {
        return String.format("https://api.github.com/repos/%s/%s/tags?per_page=100",
                this.getConfig().getOwner(), repository);
    }

    private String getUrlCommit(String repository, String sha) {
        return String.format("https://api.github.com/repos/%s/%s/commits/%s",
                this.getConfig().getOwner(), repository, sha);
    }

    private String getUrlCommitsByBranch(String repository, String branch) {
        return String.format("https://api.github.com/repos/%s/%s/commits?sha=%s&per_page=100",
                this.getConfig().getOwner(), repository, branch);
    }

    private String getUrlMembers() {
        return String.format("https://api.github.com/orgs/%s/members?per_page=100",
                this.getConfig().getOwner());
    }

    private String getUrlRepositories(int page) {
        return String.format("https://api.github.com/orgs/%s/repos?per_page=100&page=%d",
                this.getConfig().getOwner(), page);
    }

    private String getUrlCommits(String repository, String branch, Calendar since, Calendar until, Integer page) throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("per_page", 100);
        if (StringUtils.hasText(branch)) {
            map.put("sha ", branch);
        }
        if (since != null) {
            map.put("since", new SimpleDateFormat("yyyy-MM-dd").format(since.getTime()));
        }
        if (until != null) {
            map.put("until", new SimpleDateFormat("yyyy-MM-dd").format(until.getTime()));
        }
        if (page != null) {
            map.put("page", page);
        }

        return String.format("https://api.github.com/repos/%s/%s/commits?%s",
                this.getConfig().getOwner(), repository, FunctionsUtil.mapToQueryString(map));
    }

    public GithubConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (GithubConfigDTO) c.getConfig();
            }
            else {
                this.config = new GithubConfigDTO();
            }
        }
        return config;
    }

    public void setConfig(GithubConfigDTO dto) {
        this.config = dto;
    }
}
