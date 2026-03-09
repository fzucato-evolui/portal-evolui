package br.com.evolui.portalevolui.web.repository.enums;

public enum DatabaseTypeEnum {
    H2("H2"),
    ORACLE("Oracle"),
    POSTGRES("PostgreSQL"),
    SQLSERVER("Microsoft SQL Server");
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
