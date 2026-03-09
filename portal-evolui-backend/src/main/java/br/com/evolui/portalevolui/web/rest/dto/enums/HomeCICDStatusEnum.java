package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum HomeCICDStatusEnum {
    SKIPPED("IGNORADO"),
    FATAL_ERRORS("ERROS FATAIS"),
    TESTS_SKIPPED("TESTES IGNORADOS"),
    TESTS_FAILURES("TESTES COM ERROS"),
    TESTS_SUCCESS("TESTES COM SUCESSO"),
    BUILD_SKIPPED("COMPILAÇÕES IGNORADAS"),
    BUILD_FAILURES("COMPILAÇÕES COM ERROS"),
    BUILD_SUCCESS("COMPILAÇÕES COM SUCESSO");
    private final String value;

    HomeCICDStatusEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HomeCICDStatusEnum fromValue(String v) {
        for (HomeCICDStatusEnum c: HomeCICDStatusEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
