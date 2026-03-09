package br.com.evolui.portalevolui.shared.enums;

public enum CertificateTypeEnum {
    JKS("JKS"),
    PKCS12 ("PKCS12");
    private final String value;

    CertificateTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CertificateTypeEnum fromValue(String v) {
        for (CertificateTypeEnum c: CertificateTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
