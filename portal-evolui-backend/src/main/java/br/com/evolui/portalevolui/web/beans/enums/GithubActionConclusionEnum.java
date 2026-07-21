package br.com.evolui.portalevolui.web.beans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
    warning ("warning"),
    // Novos valores: sempre adicionar ao final. O enum é persistido por ordinal (sem @Enumerated(STRING)),
    // então inserir no meio deslocaria os valores já salvos no banco.
    abandoned ("abandoned"),
    unknown ("unknown");

    private final String value;
    GithubActionConclusionEnum(String v) {
        value = v;
    }

    @JsonValue
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

    @JsonCreator
    public static GithubActionConclusionEnum fromJson(String v) {
        if (v != null) {
            String normalized = v.trim().toLowerCase();
            for (GithubActionConclusionEnum c : GithubActionConclusionEnum.values()) {
                if (c.value.equals(normalized)) {
                    return c;
                }
            }
        }
        return unknown;
    }
}
