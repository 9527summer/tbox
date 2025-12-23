package org.tbox.base.core.enums;

public enum BaseStatusEnum {
    NO(0, "否"),
    YES(1, "是");

    public final int code;
    public final String meaning;

    private BaseStatusEnum(int code, String meaning) {
        this.code = code;
        this.meaning = meaning;
    }

    public static BaseStatusEnum codeOf(int code) {
        for(BaseStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }

        return null;
    }
}
