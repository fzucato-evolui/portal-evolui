package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.web.rest.intefaces.INotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationEmailConfigDTO extends NotificationBasicConfigDTO implements INotification {
    private List<String> destinations;

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
