package br.com.evolui.portalevolui.shared.enums;

public enum HealthCheckerAlertTypeEnum {
    MODULE("MODULE"),
    MEMORY("MEMORY"),
    DISK_USAGE("DISK_USAGE"),
    OPENED_FILES("OPENED_FILES"),
    CPU("CPU");
    private final String value;

    HealthCheckerAlertTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HealthCheckerAlertTypeEnum fromValue(String v) {
        for (HealthCheckerAlertTypeEnum c: HealthCheckerAlertTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
