package br.com.evolui.portalevolui.web.beans.enums;

public enum ConfigTypeEnum {
    WHATSAPP("WHATSAPP"),
    EMAIL("EMAIL"),
    REPOSITORY("REPOSITORY");
    private final String value;

    ConfigTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ConfigTypeEnum fromValue(String v) {
        for (ConfigTypeEnum c: ConfigTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
