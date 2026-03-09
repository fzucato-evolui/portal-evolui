package br.com.evolui.portalevolui.web.beans.enums;

public enum DatabaseTypeEnum {
    MSSQL("MSSQL"),
    ORACLE("ORACLE"),
    POSTGRES("POSTGRES");
    private final String value;

    DatabaseTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DatabaseTypeEnum fromValue(String v) {
        for (DatabaseTypeEnum c: DatabaseTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
