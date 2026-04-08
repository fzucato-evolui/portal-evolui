package br.com.evolui.portalevolui.web.beans.enums;

public enum ActionRDSTypeEnum {
    BACKUP("BACKUP"),
    RESTORE("RESTORE"),
    CLONE("CLONE");
    private final String value;

    ActionRDSTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ActionRDSTypeEnum fromValue(String v) {
        for (ActionRDSTypeEnum c: ActionRDSTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
