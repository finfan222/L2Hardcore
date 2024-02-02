package net.sf.l2j.gameserver.enums;

import lombok.Getter;

import java.time.LocalTime;

/**
 * @author finfan
 */
@Getter
public enum DayCycle {
    MORNING(1, LocalTime.of(6, 0), LocalTime.of(12, 0), "Утро"),
    DAY(2, LocalTime.of(12, 0), LocalTime.of(18, 0), "День"),
    EVENING(3, LocalTime.of(18, 0), LocalTime.of(23, 59), "Вечер"),
    NIGHT(4, LocalTime.of(0, 0), LocalTime.of(6, 0), "Ночь");

    private final int id;
    private final LocalTime start;
    private final LocalTime end;
    private final String name;

    DayCycle(int id, LocalTime start, LocalTime end, String name) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.name = name;
    }
}
