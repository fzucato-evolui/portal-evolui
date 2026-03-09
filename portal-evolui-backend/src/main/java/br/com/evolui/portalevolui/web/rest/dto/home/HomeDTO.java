package br.com.evolui.portalevolui.web.rest.dto.home;

import br.com.evolui.portalevolui.web.rest.dto.chart.ChartPieDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeDTO {
    private HomeGithubDTO gitHub;
    private Map<Long, ChartPieDTO> beansCountAtualizazaoVersao = new HashMap<>();
    private Map<Long, ChartPieDTO> beansCountAmbiente = new HashMap<>();
    private Map<Long, ChartPieDTO> beansCountGeracaoVersao = new HashMap<>();
    private Map<Long, ChartPieDTO> beansCountBranch = new HashMap<>();
    private List<HomeCICDDTO> cicds = new ArrayList<>();

    public HomeGithubDTO getGitHub() {
        return gitHub;
    }

    public void setGitHub(HomeGithubDTO gitHub) {
        this.gitHub = gitHub;
    }

    public Map<Long, ChartPieDTO> getBeansCountAtualizazaoVersao() {
        return beansCountAtualizazaoVersao;
    }

    public void setBeansCountAtualizazaoVersao(Map<Long, ChartPieDTO> beansCountAtualizazaoVersao) {
        this.beansCountAtualizazaoVersao = beansCountAtualizazaoVersao;
    }

    public Map<Long, ChartPieDTO> getBeansCountAmbiente() {
        return beansCountAmbiente;
    }

    public void setBeansCountAmbiente(Map<Long, ChartPieDTO> beansCountAmbiente) {
        this.beansCountAmbiente = beansCountAmbiente;
    }

    public Map<Long, ChartPieDTO> getBeansCountGeracaoVersao() {
        return beansCountGeracaoVersao;
    }

    public void setBeansCountGeracaoVersao(Map<Long, ChartPieDTO> beansCountGeracaoVersao) {
        this.beansCountGeracaoVersao = beansCountGeracaoVersao;
    }

    public Map<Long, ChartPieDTO> getBeansCountBranch() {
        return beansCountBranch;
    }

    public void setBeansCountBranch(Map<Long, ChartPieDTO> beansCountBranch) {
        this.beansCountBranch = beansCountBranch;
    }

    public List<HomeCICDDTO> getCicds() {
        return cicds;
    }

    public void setCicds(List<HomeCICDDTO> cicds) {
        this.cicds = cicds;
    }
}
