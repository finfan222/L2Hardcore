package net.sf.l2j.gameserver.enums.actors;

import lombok.Getter;

/**
 * This class defines all races that a player can choose.
 */
@Getter
public enum ClassRace {
    HUMAN(38400, 55890, 75),
    ELF(60000, 45270, 67),
    DARK_ELF(48000, 50301, 71),
    ORC(24576, 62100, 79),
    DWARF(30720, 69000, 83);

    public static final ClassRace[] VALUES = values();

    private final int breath;
    private final int weightLimit;
    private final int inventoryLimit;

    ClassRace(int breath, int weightLimit, int inventoryLimit) {
        this.breath = breath;
        this.weightLimit = weightLimit;
        this.inventoryLimit = inventoryLimit;
    }
}