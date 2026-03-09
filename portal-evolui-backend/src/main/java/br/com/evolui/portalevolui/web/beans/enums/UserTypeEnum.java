package br.com.evolui.portalevolui.web.beans.enums;

public enum UserTypeEnum {
    CUSTOM("CUSTOM"),
    GOOGLE("GOOGLE"),
    FACEBOOK("FACEBOOK");
    private final String value;

    UserTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UserTypeEnum fromValue(String v) {
        for (UserTypeEnum c: UserTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
