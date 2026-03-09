package br.com.evolui.portalevolui.web.rest.dto.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GithubRunnerLabelTypeEnum {
    READONLY("read-only"),
    CUSTOM("custom");
    private final String value;

    GithubRunnerLabelTypeEnum(String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static GithubRunnerLabelTypeEnum fromValue(String v) {
        for (GithubRunnerLabelTypeEnum c: GithubRunnerLabelTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}


