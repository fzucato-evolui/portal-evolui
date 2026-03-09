package br.com.evolui.portalevolui.shared.enums;

public enum HealthCheckerModuleTypeEnum {
    WEB("WEB"),
    EXECUTABLE("EXECUTABLE");
    private final String value;

    HealthCheckerModuleTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HealthCheckerModuleTypeEnum fromValue(String v) {
        for (HealthCheckerModuleTypeEnum c: HealthCheckerModuleTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
