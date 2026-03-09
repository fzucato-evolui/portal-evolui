package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.CICDBean;
import br.com.evolui.portalevolui.web.beans.CICDModuloBean;
import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.ProjectModuleBean;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.repository.cicd.CICDRepository;
import br.com.evolui.portalevolui.web.repository.enums.home.HomeBeanCountTypeEnum;
import br.com.evolui.portalevolui.web.repository.home.HomeRepository;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.repository.view.home.HomeBeansCountView;
import br.com.evolui.portalevolui.web.rest.dto.chart.*;
import br.com.evolui.portalevolui.web.rest.dto.enums.HomeCICDStatusEnum;
import br.com.evolui.portalevolui.web.rest.dto.github.*;
import br.com.evolui.portalevolui.web.rest.dto.home.HomeCICDDTO;
import br.com.evolui.portalevolui.web.rest.dto.home.HomeDTO;
import br.com.evolui.portalevolui.web.rest.dto.home.HomeGithubDTO;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/home")
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
public class HomeAdminRestController {
    @Autowired
    private HomeRepository repository;

    @Autowired
    private GithubVersionService githubService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CICDRepository cicdRepository;

    @GetMapping()
    public ResponseEntity<HomeDTO> get(@RequestParam Integer githubCommitDays, @RequestParam Integer beanDays, @RequestParam Integer cicdDays) throws ExecutionException, InterruptedException {
        HomeDTO dto = new HomeDTO();
        Calendar now = Calendar.getInstance();
        now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - beanDays);
        List<HomeBeansCountView> beansCount = this.repository.findAllBeansCount(now);
        {
            List<HomeBeansCountView> list = beansCount.stream().filter(x -> x.getCountType() == HomeBeanCountTypeEnum.ambiente).collect(Collectors.toList());
            for (HomeBeansCountView l : list) {
                ChartPieDTO c = new ChartPieDTO();
                if (dto.getBeansCountAmbiente() == null || !dto.getBeansCountAmbiente().containsKey(l.getProdutoId())) {
                    c.setSeries(new LinkedHashMap<>());
                    c.getSeries().put(l.getValor(), l.getTotal());
                }
                else {
                    c = dto.getBeansCountAmbiente().get(l.getProdutoId());
                    c.getSeries().put(l.getValor(), l.getTotal());
                }
                dto.getBeansCountAmbiente().put(l.getProdutoId(), c);
            }
        }
        {
            List<HomeBeansCountView> list = beansCount.stream().filter(x -> x.getCountType() == HomeBeanCountTypeEnum.branch).collect(Collectors.toList());
            for (HomeBeansCountView l : list) {
                ChartPieDTO c = new ChartPieDTO();
                if (dto.getBeansCountBranch() == null || !dto.getBeansCountBranch().containsKey(l.getProdutoId())) {
                    c.setSeries(new LinkedHashMap<>());
                    c.getSeries().put(l.getValor(), l.getTotal());
                }
                else {
                    c = dto.getBeansCountBranch().get(l.getProdutoId());
                    c.getSeries().put(l.getValor(), l.getTotal());
                }
                dto.getBeansCountBranch().put(l.getProdutoId(), c);
            }
        }
        {
            List<HomeBeansCountView> list = beansCount.stream().filter(x -> x.getCountType() == HomeBeanCountTypeEnum.geracao_versao).collect(Collectors.toList());
            for (HomeBeansCountView l : list) {
                ChartPieDTO c = new ChartPieDTO();
                if (dto.getBeansCountGeracaoVersao() == null || !dto.getBeansCountGeracaoVersao().containsKey(l.getProdutoId())) {
                    c.setSeries(new LinkedHashMap<>());
                    if (StringUtils.hasText(l.getValor())) {
                        c.getSeries().put(GithubActionConclusionEnum.values()[Integer.parseInt(l.getValor())].value(), l.getTotal());
                    }
                    else {
                        c.getSeries().put("pending", l.getTotal());
                    }
                }
                else {
                    c = dto.getBeansCountGeracaoVersao().get(l.getProdutoId());
                    if (StringUtils.hasText(l.getValor())) {
                        c.getSeries().put(GithubActionConclusionEnum.values()[Integer.parseInt(l.getValor())].value(), l.getTotal());
                    }
                    else {
                        c.getSeries().put("pending", l.getTotal());
                    }
                }
                dto.getBeansCountGeracaoVersao().put(l.getProdutoId(), c);
            }
        }
        {
            List<HomeBeansCountView> list = beansCount.stream().filter(x -> x.getCountType() == HomeBeanCountTypeEnum.atualizacao_versao).collect(Collectors.toList());
            for (HomeBeansCountView l : list) {
                ChartPieDTO c = new ChartPieDTO();
                if (dto.getBeansCountAtualizazaoVersao() == null || !dto.getBeansCountAtualizazaoVersao().containsKey(l.getProdutoId())) {
                    c.setSeries(new LinkedHashMap<>());
                    if (StringUtils.hasText(l.getValor())) {
                        c.getSeries().put(GithubActionConclusionEnum.values()[Integer.parseInt(l.getValor())].value(), l.getTotal());
                    }
                    else {
                        c.getSeries().put("pending", l.getTotal());
                    }
                }
                else {
                    c = dto.getBeansCountAtualizazaoVersao().get(l.getProdutoId());
                    if (StringUtils.hasText(l.getValor())) {
                        c.getSeries().put(GithubActionConclusionEnum.values()[Integer.parseInt(l.getValor())].value(), l.getTotal());
                    }
                    else {
                        c.getSeries().put("pending", l.getTotal());
                    }
                }
                dto.getBeansCountAtualizazaoVersao().put(l.getProdutoId(), c);
            }
        }
        now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - cicdDays);
        List<CICDBean> cicds = this.cicdRepository.findAllByConclusionDateAfterOrderByProjectIdAscBranchAscConclusionDateAsc(now);

        if(cicds != null && !cicds.isEmpty()) {
            List<HomeCICDDTO> dtos = new ArrayList<>();
            for(CICDBean cicd : cicds) {
                if (cicd.getConclusion() == null) {
                    continue;
                }
                HomeCICDDTO dtoCicd = dtos.stream().filter(x -> x.getBranch().equals(cicd.getBranch()) && x.getProdutoId().equals(cicd.getProject().getId())).findFirst().orElse(null);
                if (dtoCicd == null) {
                    dtoCicd = new HomeCICDDTO();
                    dtoCicd.setBranch(cicd.getBranch());
                    dtoCicd.setProdutoId(cicd.getProject().getId());
                    ChartStackedBarDTO chart = new ChartStackedBarDTO();
                    chart.setLabels(new ArrayList<>());
                    for (HomeCICDStatusEnum c: HomeCICDStatusEnum.values()) {
                        chart.getLabels().add(c.value());
                    }
                    chart.setSeries(new ArrayList<>());
                    dtoCicd.setChart(chart);
                    dtos.add(dtoCicd);
                }
                ChartStackedBarValueDTO value = new ChartStackedBarValueDTO();
                value.setY(new LinkedHashMap<>());
                value.setX(cicd.getConclusionDate());
                dtoCicd.getChart().getSeries().add(value);
                if (cicd.getConclusion() == GithubActionConclusionEnum.skipped) {
                    value.getY().put(HomeCICDStatusEnum.SKIPPED.value(), 1);
                    continue;
                }
                for (CICDModuloBean modulo : cicd.getModules()) {
                    if (!modulo.isEnabled()) {
                        if (!value.getY().containsKey(HomeCICDStatusEnum.BUILD_SKIPPED.value())) {
                            value.getY().put(HomeCICDStatusEnum.BUILD_SKIPPED.value(), 1);
                        }
                        else {
                            value.getY().put(HomeCICDStatusEnum.BUILD_SKIPPED.value(), (Integer)value.getY().get(HomeCICDStatusEnum.BUILD_SKIPPED.value()) + 1);
                        }
                        continue;
                    }
                    if (modulo.getFatalError() != null && modulo.getFatalError().booleanValue()) {
                        if (!value.getY().containsKey(HomeCICDStatusEnum.FATAL_ERRORS.value())) {
                            value.getY().put(HomeCICDStatusEnum.FATAL_ERRORS.value(), 1);
                        }
                        else {
                            value.getY().put(HomeCICDStatusEnum.FATAL_ERRORS.value(), (Integer)value.getY().get(HomeCICDStatusEnum.FATAL_ERRORS.value()) + 1);
                        }
                        continue;
                    }
                    {
                        GithubCICDModuleResultDTO.Summary sumary = modulo.getTestSumary();
                        if (sumary != null) {

                            if (sumary.getSkipped() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.TESTS_SKIPPED.value())) {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_SKIPPED.value(), sumary.getSkipped());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_SKIPPED.value(), (Integer) value.getY().get(HomeCICDStatusEnum.TESTS_SKIPPED.value()) + sumary.getSkipped());
                                }
                            }
                            if (sumary.getFailure() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.TESTS_FAILURES.value())) {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_FAILURES.value(), sumary.getFailure());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_FAILURES.value(), (Integer) value.getY().get(HomeCICDStatusEnum.TESTS_FAILURES.value()) + sumary.getFailure());
                                }
                            }
                            if (sumary.getSuccess() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.TESTS_SUCCESS.value())) {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_SUCCESS.value(), sumary.getSuccess());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.TESTS_SUCCESS.value(), (Integer) value.getY().get(HomeCICDStatusEnum.TESTS_SUCCESS.value()) + sumary.getSuccess());
                                }
                            }
                        }
                    }
                    {
                        GithubCICDModuleResultDTO.Summary sumary = modulo.getBuildSumary();
                        if (sumary != null) {

                            if (sumary.getSkipped() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.BUILD_SKIPPED.value())) {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_SKIPPED.value(), sumary.getSkipped());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_SKIPPED.value(), (Integer) value.getY().get(HomeCICDStatusEnum.BUILD_SKIPPED.value()) + sumary.getSkipped());
                                }
                            }
                            if (sumary.getFailure() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.BUILD_FAILURES.value())) {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_FAILURES.value(), sumary.getFailure());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_FAILURES.value(), (Integer) value.getY().get(HomeCICDStatusEnum.BUILD_FAILURES.value()) + sumary.getFailure());
                                }
                            }
                            if (sumary.getSuccess() != null) {
                                if (!value.getY().containsKey(HomeCICDStatusEnum.BUILD_SUCCESS.value())) {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_SUCCESS.value(), sumary.getSuccess());
                                } else {
                                    value.getY().put(HomeCICDStatusEnum.BUILD_SUCCESS.value(), (Integer) value.getY().get(HomeCICDStatusEnum.BUILD_SUCCESS.value()) + sumary.getSuccess());
                                }
                            }
                        }
                    }
                }

            }
            dto.setCicds(dtos);
        }

        dto.setGitHub(new HomeGithubDTO());

        List<ProjectBean> projects = this.projectRepository.findAll();
        List<Future<Map.Entry<Long,GithubCommitStatisticDTO>>> callables = new ArrayList<>();
        for (ProjectBean project : projects) {
            if (project.isFramework()) {
                //continue;
            }
            Set<String> repositories = new HashSet<>();
            repositories.add(project.getRepository());
            for (ProjectModuleBean module : project.getModules()) {
                if (!module.isFramework()) {
                    repositories.add(project.getRepository());
                }
            }
            callables.add(this.getGithubStatisticProduct(repositories, project.getId(), githubCommitDays));
        }
        Future<List<GithubRunnerDTO>> frunners = this.getGithubRunners();
        Future<List<GithubDetailedRepositoryDTO>> frepos = this.getGithubRepositories();
        Future<List<GithubMemberDTO>> fmembers = this.getGithubMembers();

        List<GithubRunnerDTO> runners = frunners.get();
        dto.getGitHub().setRunners(new ChartPieDTO());
        dto.getGitHub().getRunners().setSeries(new LinkedHashMap<>());
        for (GithubRunnerDTO r : runners) {
            if (!dto.getGitHub().getRunners().getSeries().containsKey(r.getStatus())) {
                dto.getGitHub().getRunners().getSeries().put(r.getStatus(), 1);
            }
            else {
                Integer value = (Integer)dto.getGitHub().getRunners().getSeries().get(r.getStatus()) + 1;
                dto.getGitHub().getRunners().getSeries().put(r.getStatus(), value);
            }

        }

        List<GithubDetailedRepositoryDTO> repositories = frepos.get();
        dto.getGitHub().setRepositories(new ChartPieDTO());
        dto.getGitHub().getRepositories().setSeries(new LinkedHashMap<>());
        for (GithubDetailedRepositoryDTO r : repositories) {
            if (r.isDisabled()) {
                if (!dto.getGitHub().getRepositories().getSeries().containsKey("disabled")) {
                    dto.getGitHub().getRepositories().getSeries().put("disabled", 1);
                }
                else {
                    Integer value = (Integer)dto.getGitHub().getRepositories().getSeries().get("disabled") + 1;
                    dto.getGitHub().getRepositories().getSeries().put("disabled", value);
                }
            }
            else {
                if (!dto.getGitHub().getRepositories().getSeries().containsKey("enabled")) {
                    dto.getGitHub().getRepositories().getSeries().put("enabled", 1);
                }
                else {
                    Integer value = (Integer)dto.getGitHub().getRepositories().getSeries().get("enabled") + 1;
                    dto.getGitHub().getRepositories().getSeries().put("enabled", value);
                }
            }


        }

        List<GithubMemberDTO> members = fmembers.get();
        dto.getGitHub().setMembers(new ChartPieDTO());
        dto.getGitHub().getMembers().setSeries(new LinkedHashMap<>());
        for (GithubMemberDTO r : members) {
            if (r.getSite_admin() != null && r.getSite_admin().booleanValue()) {
                if (!dto.getGitHub().getMembers().getSeries().containsKey("admin")) {
                    dto.getGitHub().getMembers().getSeries().put("admin", 1);
                }
                else {
                    Integer value = (Integer)dto.getGitHub().getMembers().getSeries().get("admin") + 1;
                    dto.getGitHub().getMembers().getSeries().put("admin", value);
                }
            }
            else {
                if (!dto.getGitHub().getMembers().getSeries().containsKey("user")) {
                    dto.getGitHub().getMembers().getSeries().put("user", 1);
                }
                else {
                    Integer value = (Integer)dto.getGitHub().getMembers().getSeries().get("user") + 1;
                    dto.getGitHub().getMembers().getSeries().put("user", value);
                }
            }


        }
        dto.getGitHub().setProductCommits(new ChartLineDTO());
        dto.getGitHub().getProductCommits().setSeries(new LinkedHashMap<>());
        dto.getGitHub().setMainCommiters(new LinkedHashMap<>());
        for (Future<Map.Entry<Long,GithubCommitStatisticDTO>> c : callables) {
            try {
                Map.Entry<Long, GithubCommitStatisticDTO> l = c.get();
                l.getValue().getPerAuthor().entrySet().stream().forEach(x -> {
                    ChartPieDTO chart = new ChartPieDTO();
                    if (!dto.getGitHub().getMainCommiters().containsKey(l.getKey())) {

                        chart.setSeries(new LinkedHashMap<>());
                        chart.getSeries().put(x.getValue().getAuthor().getEmail(), x.getValue().getCount());
                    } else {
                        chart = dto.getGitHub().getMainCommiters().get(l.getKey());
                        chart.getSeries().put(x.getValue().getAuthor().getEmail(), x.getValue().getCount());
                    }
                    dto.getGitHub().getMainCommiters().put(l.getKey(), chart);
                });

                l.getValue().getPerDay().entrySet().stream().forEach(x -> {
                    ChartXYDTO coordinate = new ChartXYDTO();
                    coordinate.setX(x.getKey());
                    coordinate.setY(x.getValue());

                    List<ChartXYDTO> coordinates = new ArrayList<>();
                    if (dto.getGitHub().getProductCommits().getSeries().containsKey(l.getKey().toString())) {
                        coordinates = dto.getGitHub().getProductCommits().getSeries().get(l.getKey().toString());

                    }

                    coordinates.add(coordinate);
                    dto.getGitHub().getProductCommits().getSeries().put(l.getKey().toString(), coordinates);
                });
            }
            catch (Exception ex) {
                return null;
            }

        }

        return ResponseEntity.ok(dto);
    }

    Future<List<GithubRunnerDTO>> getGithubRunners() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<GithubRunnerDTO>> callback = executor.submit(() -> {
            try {
                return githubService.getRunners().getRunners();
            }
            catch (Exception ex) {
                return new ArrayList<>();
            }
        });
        return callback;
    }

    Future<List<GithubMemberDTO>> getGithubMembers() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<GithubMemberDTO>> callback = executor.submit(() -> {
            try {
                return githubService.getMembers();
            }
            catch (Exception ex) {
                return new ArrayList<>();
            }
        });
        return callback;
    }

    Future<List<GithubDetailedRepositoryDTO>> getGithubRepositories() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<GithubDetailedRepositoryDTO>> callback = executor.submit(() -> {
            try {
                return githubService.getRepositories();
            }
            catch (Exception ex) {
                return new ArrayList<>();
            }
        });
        return callback;
    }

    Future<Map.Entry<Long,GithubCommitStatisticDTO>> getGithubStatisticProduct(final Set<String> repositories, final Long productId, final Integer totalDays) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map.Entry<Long,GithubCommitStatisticDTO>> callback = executor.submit(() -> {
            List<GithubCommitHistoryDTO> histories = new ArrayList<>();
            Calendar now = null;
            if (totalDays != null) {
                now = Calendar.getInstance();
                now.set(Calendar.HOUR_OF_DAY, 0);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);
                now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - totalDays);
            }
            for(String repo : repositories) {
                histories.addAll(githubService.getCommitHistory(repo, null, now, null));
            }
            GithubCommitStatisticDTO dto = GithubCommitStatisticDTO.parseFromHistory(histories);
            Map.Entry<Long, GithubCommitStatisticDTO> entry = new AbstractMap.SimpleEntry<>(productId, dto);

            return entry;
        });
        return callback;
    }

}
