package br.com.evolui.portalevolui.web.rest.dto.enums;

public enum NotificationTypeEnum {
    WHATSAPP("WHATSAPP"),
    GOOGLE_CHAT("GOOGLE_CHAT"),
    MONDAY_BOARD("MONDAY_BOARD"),
    EMAIL("EMAIL");
    private final String value;

    NotificationTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NotificationTypeEnum fromValue(String v) {
        for (NotificationTypeEnum c: NotificationTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
