package br.com.evolui.portalevolui.web.rest.dto.chart;

import java.util.LinkedHashMap;
import java.util.List;

public class ChartLineDTO {
    private LinkedHashMap<String, List<ChartXYDTO>> series;

    public LinkedHashMap<String, List<ChartXYDTO>> getSeries() {
        return series;
    }

    public void setSeries(LinkedHashMap<String, List<ChartXYDTO>> series) {
        this.series = series;
    }
}
