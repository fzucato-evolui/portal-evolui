package br.com.evolui.portalevolui.web.beans.enums;

public enum CompileTypeEnum {
    patch("patch"),
    stable("stable"),
    rc("rc"),
    beta ("beta"),
    alpha ("alpha");

    private final String value;

    CompileTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CompileTypeEnum fromValue(String v) {
        for (CompileTypeEnum c: CompileTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static boolean isTransitoryType(CompileTypeEnum value) {
        return value != patch && value != stable;
    }
}
