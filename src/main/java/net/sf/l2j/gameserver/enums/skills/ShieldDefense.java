package net.sf.l2j.gameserver.enums.skills;

import lombok.Getter;

@Getter
public enum ShieldDefense {

    FAILED(false),
    SUCCESS(true),
    PERFECT(true);

    private final boolean isSuccess;

    ShieldDefense(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}