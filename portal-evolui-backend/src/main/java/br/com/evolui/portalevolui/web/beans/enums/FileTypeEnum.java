package br.com.evolui.portalevolui.web.beans.enums;

public enum FileTypeEnum {
    AUDIO("AUDIO"),
    ZIP("ZIP"),
    DOCUMENT("DOCUMENT"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    UNKNOW("UNKNOWN");
    private final String value;

    FileTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static FileTypeEnum fromValue(String v) {
        for (FileTypeEnum c: FileTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
