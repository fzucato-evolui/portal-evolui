package br.com.evolui.portalevolui.web.rest.dto.config;

import java.util.ArrayList;
import java.util.List;

public class NotificationBasicConfigDTO {
    protected Boolean enabled;
    private List<String> destinations;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public void addDestination(String destination) {
        if (this.destinations == null) {
            this.destinations = new ArrayList<>();
        }
        this.destinations.add(destination);
    }
}
