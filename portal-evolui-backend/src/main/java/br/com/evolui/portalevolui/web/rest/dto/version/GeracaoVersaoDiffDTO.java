package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.beans.GeracaoVersaoBean;

import java.util.ArrayList;
import java.util.List;

public class GeracaoVersaoDiffDTO  {
    private GeracaoVersaoBean from;
    private GeracaoVersaoBean to;
    private List<GeracaoVersaoDiffModuleDTO> modulesDiff;

    public GeracaoVersaoBean getFrom() {
        return from;
    }

    public void setFrom(GeracaoVersaoBean from) {
        this.from = from;
    }

    public GeracaoVersaoBean getTo() {
        return to;
    }

    public void setTo(GeracaoVersaoBean to) {
        this.to = to;
    }

    public List<GeracaoVersaoDiffModuleDTO> getModulesDiff() {
        return modulesDiff;
    }

    public void setModulesDiff(List<GeracaoVersaoDiffModuleDTO> modulesDiff) {
        this.modulesDiff = modulesDiff;
    }

    public void addDiffModule(GeracaoVersaoDiffModuleDTO diff) {
        if (this.modulesDiff == null) {
            this.modulesDiff = new ArrayList<>();
        }
        this.modulesDiff.add(diff);
    }
}
