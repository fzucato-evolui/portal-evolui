package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum AWSInstanceRunnerTypeEnum {
    EC2("EC2"),
    WORKSPACE("WORKSPACE");
    private final String value;

    AWSInstanceRunnerTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AWSInstanceRunnerTypeEnum fromValue(String v) {
        for (AWSInstanceRunnerTypeEnum c: AWSInstanceRunnerTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
