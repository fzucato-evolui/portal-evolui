package br.com.evolui.portalevolui.web.beans.enums;

public enum GithubActionStatusEnum {
    queued ("queued"),
    in_progress ("in_progress"),
    completed ("completed"),
    scheduled ("scheduled"),
    canceling ("canceling");

    private final String value;
    GithubActionStatusEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static GithubActionStatusEnum fromValue(String v) {
        for (GithubActionStatusEnum c: GithubActionStatusEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
