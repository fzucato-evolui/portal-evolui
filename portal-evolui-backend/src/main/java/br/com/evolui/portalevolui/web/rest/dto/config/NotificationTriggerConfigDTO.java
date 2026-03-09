package br.com.evolui.portalevolui.web.rest.dto.config;

import br.com.evolui.portalevolui.web.rest.dto.enums.NotificationTriggerEnum;
import br.com.evolui.portalevolui.web.rest.dto.enums.NotificationTypeEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NotificationTriggerConfigDTO  {
    private NotificationTriggerEnum triggerType;
    private List<String> references;
    private LinkedHashMap<NotificationTypeEnum, NotificationBasicConfigDTO> configs = new LinkedHashMap<>();

    public NotificationTriggerEnum getTriggerType() {
        return this.triggerType;
    }

    public void setTriggerType(NotificationTriggerEnum triggerType) {
        this.triggerType = triggerType;
    }

    public List<String> getReferences() {
        return this.references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public void addReference(String reference) {
        if (this.references == null) {
            this.references = new ArrayList<>();
        }
        this.references.add(reference);
    }

    public LinkedHashMap<NotificationTypeEnum, NotificationBasicConfigDTO> getConfigs() {
        return configs;
    }

    public void setConfigs(LinkedHashMap<NotificationTypeEnum, NotificationBasicConfigDTO> configs) {
        this.configs = configs;
    }

    public void putConfig(NotificationTypeEnum type, NotificationBasicConfigDTO config) {
        if (this.configs == null) {
            this.configs = new LinkedHashMap<>();
        }
        this.configs.put(type, config);
    }

}


