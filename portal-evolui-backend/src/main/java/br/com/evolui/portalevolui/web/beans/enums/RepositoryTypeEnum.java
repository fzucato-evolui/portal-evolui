package br.com.evolui.portalevolui.web.beans.enums;

public enum RepositoryTypeEnum {
    AWS("AWS"),
    GOOGLE("GOOGLE"),
    SERVERFOLDER("SERVERFOLDER");
    private final String value;

    RepositoryTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RepositoryTypeEnum fromValue(String v) {
        for (RepositoryTypeEnum c: RepositoryTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
