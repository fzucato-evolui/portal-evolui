package br.com.evolui.portalevolui.web.beans.enums;

public enum GithubActionConclusionEnum {
    success ("success"),
    failure ("failure"),
    neutral ("neutral"),
    cancelled ("cancelled"),
    skipped ("skipped"),
    timed_out ("timed_out"),
    action_required ("action_required"),
    startup_failure ("startup_failure"),
    stale ("stale"),
    cancelling ("cancelling"),
    scheduler_error("scheduler_error"),
    warning ("warning");

    private final String value;
    GithubActionConclusionEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static GithubActionConclusionEnum fromValue(String v) {
        for (GithubActionConclusionEnum c: GithubActionConclusionEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
