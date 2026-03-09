package br.com.evolui.portalevolui.web.rest.dto;

import br.com.evolui.portalevolui.shared.dto.HealthCheckerAlertConfigDTO;
import br.com.evolui.portalevolui.shared.dto.HealthCheckerAlertDTO;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerAlertTypeEnum;
import br.com.evolui.portalevolui.web.beans.HealthCheckerBean;
import br.com.evolui.portalevolui.web.beans.HealthCheckerModuloBean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HealthCheckerEmailNotificationDTO {
    private String title;
    private String link;
    private boolean health;
    private List<ModulesDTO> modules;
    private String author;
    private String requestDate;
    private String lastHealthDate;
    private String subject;
    private List<String> destinations;
    private List<AlertDTO> alerts;
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isHealth() {
        return health;
    }

    public void setHealth(boolean health) {
        this.health = health;
    }

    public List<ModulesDTO> getModules() {
        return modules;
    }

    public void setModules(List<ModulesDTO> modules) {
        this.modules = modules;
    }

    public void addModule(ModulesDTO module) {
        if(this.modules == null) {
            this.modules = new ArrayList<>();
        }
        this.modules.add(module);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getLastHealthDate() {
        return lastHealthDate;
    }

    public void setLastHealthDate(String lastHealthDate) {
        this.lastHealthDate = lastHealthDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public void addDestination(String destination) {
        if(this.destinations == null) {
            this.destinations = new ArrayList<>();
        }
        this.destinations.add(destination);
    }

    public List<AlertDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertDTO> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(AlertDTO alert) {
        if (this.alerts == null) {
            this.alerts = new ArrayList<>();
        }
        this.alerts.add(alert);
    }
    
    public static HealthCheckerEmailNotificationDTO fromBean(HealthCheckerBean bean, HealthCheckerBean body, List<String> destination, String link) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        HealthCheckerEmailNotificationDTO dto = new HealthCheckerEmailNotificationDTO();
        boolean needNotification = false;
        if (body.getHealth() != null) {
            if (bean.getHealth() != null && bean.getHealth().booleanValue() != body.getHealth().booleanValue() ||
                    bean.getHealth() == null && !body.getHealth().booleanValue()) {

                for(Map.Entry<HealthCheckerAlertTypeEnum, HealthCheckerAlertConfigDTO> e : bean.getConfig().getAlerts().entrySet()) {
                    if (e.getValue().getMaxPercentual() <= 0 || !e.getValue().isSendNotification() ||
                            body.getConfig() == null || body.getConfig().getAlerts() == null || body.getConfig().getAlerts().isEmpty()) {
                        continue;
                    }
                    List<HealthCheckerAlertDTO> alerts = body.getAlerts() == null ? null : body.getAlerts().stream().filter(x -> x.getAlertType() == e.getKey()).collect(Collectors.toList());
                    List<HealthCheckerAlertDTO> alertsBean = bean.getAlerts() == null ? null : bean.getAlerts().stream().filter(x -> x.getAlertType() == e.getKey()).collect(Collectors.toList());
                    if (alerts != null && !alerts.isEmpty()) {
                        for (int i = 0; i < alerts.size(); i++) {
                            HealthCheckerAlertDTO alert = alerts.get(i);

                            if (alertsBean != null && !alertsBean.isEmpty() && i < alertsBean.size() - 1) {
                                if (alertsBean.get(i).isHealth() == alert.isHealth()) {
                                    continue;
                                }
                            }
                            needNotification = true;
                            AlertDTO alertDTO = new AlertDTO();
                            alertDTO.alertType = e.getKey().name();
                            alertDTO.setHealth(alert.isHealth());
                            alertDTO.setMessage(alert.isHealth() ? "Voltou ao normal" : alert.getError());
                            dto.addAlert(alertDTO);
                        }
                    }
                    // Tinha alerta de memória, disco e agora não tem mais
                    else if (alertsBean != null && !alertsBean.isEmpty()) {
                        for (int i = 0; i < alertsBean.size(); i++) {
                            needNotification = true;
                            AlertDTO alertDTO = new AlertDTO();
                            alertDTO.alertType = e.getKey().name();
                            alertDTO.setHealth(true);
                            alertDTO.setMessage("Voltou ao normal");
                            dto.addAlert(alertDTO);
                        }
                    }
                }

            }
        }
        dto.setHealth(body.getHealth());
        dto.setRequestDate(sdf.format(body.getLastUpdate().getTime()));
        dto.setLastHealthDate(sdf.format(body.getLastHealthDate().getTime()));
        dto.setLink(link);
        dto.setAuthor(bean.getUser().getName());
        dto.setSubject("Alerta HealthChecker");
        dto.setTitle("Alertas HealthChecker " + bean.getDescription());
        dto.setDestinations(destination);
        for (HealthCheckerModuloBean moduloBean : bean.getModules()) {
            if (!moduloBean.getConfig().isSendNotification()) {
                continue;
            }
            HealthCheckerModuloBean moduloBody = body.getModules().stream().filter(x -> x.getId().equals(moduloBean.getId())).findFirst().orElse(null);
            if (moduloBody == null || moduloBody.getHealth() == null) {
                continue;
            }
            if (moduloBean.getHealth() != null && moduloBean.getHealth().booleanValue() != moduloBody.getHealth().booleanValue() ||
                    moduloBean.getHealth() == null && !moduloBody.getHealth().booleanValue()) {
                ModulesDTO modulesDTO = new ModulesDTO();
                modulesDTO.setHealth(moduloBody.getHealth());
                modulesDTO.setLastHealthDate(sdf.format(moduloBody.getLastHealthDate().getTime()));
                modulesDTO.setName(moduloBody.getIdentifier());
                modulesDTO.setMessage(moduloBody.getHealth().booleanValue() ? "Voltou ao normal" : "Falha na verificação");
                dto.addModule(modulesDTO);
                needNotification = true;
            }
        }
        if (needNotification) {
            return dto;
        } else {
            return null;
        }
    }


    public static class ModulesDTO {
        private boolean health;
        private String name;
        private String message;
        private String lastHealthDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLastHealthDate() {
            return lastHealthDate;
        }

        public void setLastHealthDate(String lastHealthDate) {
            this.lastHealthDate = lastHealthDate;
        }

        public boolean isHealth() {
            return health;
        }

        public void setHealth(boolean health) {
            this.health = health;
        }
    }

    public static class AlertDTO {
        private String alertType;
        private String message;
        private boolean health;

        public String getAlertType() {
            return alertType;
        }

        public void setAlertType(String alertType) {
            this.alertType = alertType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isHealth() {
            return health;
        }

        public void setHealth(boolean health) {
            this.health = health;
        }
    }
}
