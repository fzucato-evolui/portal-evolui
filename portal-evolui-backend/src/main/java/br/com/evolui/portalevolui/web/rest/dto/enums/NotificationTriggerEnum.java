package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum NotificationTriggerEnum {
    VERSION_CREATION("VERSION_CREATION"),
    VERSION_UPDATE("VERSION_UPDATE"),
    CI_CD("CI_CD"),
    HEALTH_CHECKER("HEALTH_CHECKER"),
    BACKUP_RESTORE("BACKUP_RESTORE");
    private final String value;

    NotificationTriggerEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NotificationTriggerEnum fromValue(String v) {
        for (NotificationTriggerEnum c: NotificationTriggerEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
