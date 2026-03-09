package br.com.evolui.portalevolui.web.rest.dto.home;

import br.com.evolui.portalevolui.web.rest.dto.chart.ChartStackedBarDTO;

public class HomeCICDDTO {
    private Long produtoId;
    private String branch;
    private ChartStackedBarDTO chart;

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public ChartStackedBarDTO getChart() {
        return chart;
    }

    public void setChart(ChartStackedBarDTO chart) {
        this.chart = chart;
    }
}
