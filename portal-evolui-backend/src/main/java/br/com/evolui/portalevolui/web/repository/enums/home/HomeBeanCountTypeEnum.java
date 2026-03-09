package br.com.evolui.portalevolui.web.repository.enums.home;

public enum HomeBeanCountTypeEnum {
    ambiente("ambiente"),
    atualizacao_versao("atualizacao_versao"),
    geracao_versao("geracao_versao"),
    branch("branch");
    private final String value;

    HomeBeanCountTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HomeBeanCountTypeEnum fromValue(String v) {
        for (HomeBeanCountTypeEnum c: HomeBeanCountTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
