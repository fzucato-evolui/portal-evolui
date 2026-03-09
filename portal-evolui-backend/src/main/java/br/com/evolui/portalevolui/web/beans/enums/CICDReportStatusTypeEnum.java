package br.com.evolui.portalevolui.web.beans.enums;

public enum CICDReportStatusTypeEnum {
    SUCCESS ("SUCCESS"),
    SKIPPED("SKIPPED"),
    FAILURE("FAILURE");

    private final String value;

    CICDReportStatusTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CICDReportStatusTypeEnum fromValue(String v) {
        for (CICDReportStatusTypeEnum c: CICDReportStatusTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}

