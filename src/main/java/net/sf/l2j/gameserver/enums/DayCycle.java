package net.sf.l2j.gameserver.enums;

import lombok.Getter;

/**
 * @author finfan
 */
@Getter
public enum DayCycle {
    MORNING(1, 360, 720, "Утро"),
    DAY(2, 720, 1080, "День"),
    EVENING(3, 1080, 1440, "Вечер"),
    NIGHT(4, 0, 360, "Ночь");

    private final int id;
    private final int start;
    private final int end;
    private final String name;

    DayCycle(int id, int start, int end, String name) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.name = name;
    }
}
