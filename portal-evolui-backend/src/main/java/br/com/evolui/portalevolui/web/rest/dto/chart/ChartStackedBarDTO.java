package br.com.evolui.portalevolui.web.rest.dto.chart;

import java.util.List;

public class ChartStackedBarDTO {
    private List<String> labels;
    private List<ChartStackedBarValueDTO> series;

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ChartStackedBarValueDTO> getSeries() {
        return series;
    }

    public void setSeries(List<ChartStackedBarValueDTO> series) {
        this.series = series;
    }
}
