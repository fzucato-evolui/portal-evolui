package br.com.evolui.portalevolui.web.beans.enums;

public enum SystemConfigTypeEnum {
    GOOGLE("GOOGLE"),
    GENERAL("GENERAL"),
    AWS ("AWS"),
    GITHUB ("GITHUB"),
    SMTP ("SMTP"),
    NOTIFICATION ("NOTIFICATION"),
    MONDAY ("MONDAY"),
    CICD ("CICD"),
    AX ("AX"),
    PORTAL_LUTHIER ("PORTAL_LUTHIER");

    private final String value;

    SystemConfigTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SystemConfigTypeEnum fromValue(String v) {
        for (SystemConfigTypeEnum c: SystemConfigTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
