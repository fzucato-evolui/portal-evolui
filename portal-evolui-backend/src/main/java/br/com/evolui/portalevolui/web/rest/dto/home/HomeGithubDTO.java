package br.com.evolui.portalevolui.web.rest.dto.home;

import br.com.evolui.portalevolui.web.rest.dto.chart.ChartLineDTO;
import br.com.evolui.portalevolui.web.rest.dto.chart.ChartPieDTO;

import java.util.LinkedHashMap;

public class HomeGithubDTO {
    private ChartPieDTO runners;
    private ChartPieDTO repositories;
    private ChartPieDTO members;
    private ChartLineDTO productCommits;
    private LinkedHashMap<Long, ChartPieDTO> mainCommiters;


    public ChartPieDTO getRunners() {
        return runners;
    }

    public void setRunners(ChartPieDTO runners) {
        this.runners = runners;
    }

    public ChartPieDTO getRepositories() {
        return repositories;
    }

    public void setRepositories(ChartPieDTO repositories) {
        this.repositories = repositories;
    }

    public ChartPieDTO getMembers() {
        return members;
    }

    public void setMembers(ChartPieDTO members) {
        this.members = members;
    }

    public ChartLineDTO getProductCommits() {
        return productCommits;
    }

    public void setProductCommits(ChartLineDTO productCommits) {
        this.productCommits = productCommits;
    }

    public LinkedHashMap<Long, ChartPieDTO> getMainCommiters() {
        return mainCommiters;
    }

    public void setMainCommiters(LinkedHashMap<Long, ChartPieDTO> mainCommiters) {
        this.mainCommiters = mainCommiters;
    }
}
