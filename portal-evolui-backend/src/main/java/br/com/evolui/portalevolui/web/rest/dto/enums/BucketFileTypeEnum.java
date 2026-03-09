package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum BucketFileTypeEnum {
    BUCKET("BUCKET"),
    DIRECTORY("DIRECTORY"),
    FILE("FILE");
    private final String value;

    BucketFileTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static BucketFileTypeEnum fromValue(String v) {
        for (BucketFileTypeEnum c: BucketFileTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
