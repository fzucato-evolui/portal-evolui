package br.com.evolui.portalevolui.web.rest.dto.chart;

import java.util.LinkedHashMap;

public class ChartPieDTO {
    private LinkedHashMap<String, Object> series;

    public LinkedHashMap<String, Object> getSeries() {
        return series;
    }

    public void setSeries(LinkedHashMap<String, Object> series) {
        this.series = series;
    }
}
