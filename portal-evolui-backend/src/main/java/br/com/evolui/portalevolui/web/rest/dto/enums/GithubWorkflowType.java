package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum GithubWorkflowType {
    builder ("evolui_build"),
    updater("evolui_update"),
    tester("evolui_test");

    private final String value;

    GithubWorkflowType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static GithubWorkflowType fromValue(String v) {
        for (GithubWorkflowType c: GithubWorkflowType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}

