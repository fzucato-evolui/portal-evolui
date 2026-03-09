package br.com.evolui.portalevolui.web.beans.enums;

public enum AWSActionTypeEnum {
    START_EC2("START_EC2"),
    STOP_EC2("STOP_EC2"),
    START_RDS("START_RDS"),
    STOP_RDS("STOP_RDS"),
    START_WORKSPACE("START_WORKSPACE"),
    REBOOT_WORKSPACE("REBOOT_WORKSPACE"),
    STOP_WORKSPACE("STOP_WORKSPACE"),
    REBOOT_EC2("REBOOT_EC2"),
    RESTORE_RDS("RESTORE_RDS"),
    BACKUP_RDS("BACKUP_RDS");
    private final String value;

    AWSActionTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AWSActionTypeEnum fromValue(String v) {
        for (AWSActionTypeEnum c: AWSActionTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
