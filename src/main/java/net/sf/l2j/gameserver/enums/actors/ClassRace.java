package net.sf.l2j.gameserver.enums.actors;

import lombok.Getter;

/**
 * This class defines all races that a player can choose.
 */
@Getter
public enum ClassRace {
    HUMAN(38400, 84825),
    ELF(60000, 76546),
    DARK_ELF(48000, 81432),
    ORC(24576, 90240),
    DWARF(30720, 96000);

    public static final ClassRace[] VALUES = values();

    private final int breath;
    private final int weightLimit;

    ClassRace(int breath, int weightLimit) {
        this.breath = breath;
        this.weightLimit = weightLimit;
    }
}