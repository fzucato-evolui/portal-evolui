package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum AWSAccountTypeEnum {
    DEV("DEV"),
    PROD("PROD"),
    ROOT("ROOT");
    private final String value;

    AWSAccountTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AWSAccountTypeEnum fromValue(String v) {
        for (AWSAccountTypeEnum c: AWSAccountTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
