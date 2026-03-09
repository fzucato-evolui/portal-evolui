package br.com.evolui.portalevolui.web.rest.dto.chart;

import java.util.LinkedHashMap;

public class ChartStackedBarValueDTO {
    private Object x;
    private LinkedHashMap<String, Object> y;

    public Object getX() {
        return x;
    }

    public void setX(Object x) {
        this.x = x;
    }


    public LinkedHashMap<String, Object> getY() {
        return y;
    }

    public void setY(LinkedHashMap<String, Object> y) {
        this.y = y;
    }
}
