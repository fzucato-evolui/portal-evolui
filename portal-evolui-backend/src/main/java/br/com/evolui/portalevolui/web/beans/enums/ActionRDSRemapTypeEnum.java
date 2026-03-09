package br.com.evolui.portalevolui.web.beans.enums;

public enum ActionRDSRemapTypeEnum {
    SCHEMA("SCHEMA"),
    TABLESPACE("TABLESPACE"),
    DUMP_DIR("DUMP_DIR");
    private final String value;

    ActionRDSRemapTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ActionRDSRemapTypeEnum fromValue(String v) {
        for (ActionRDSRemapTypeEnum c: ActionRDSRemapTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
