package br.com.evolui.portalevolui.web.beans.enums;

public enum ProfileEnum {
    ROLE_HYPER("ROLE_HYPER"),
    ROLE_SUPER("ROLE_SUPER"),
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_HEALTHCHECKER("ROLE_HEALTHCHECKER"),
    ROLE_USER("ROLE_USER");
    private final String value;

    ProfileEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ProfileEnum fromValue(String v) {
        for (ProfileEnum c: ProfileEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
