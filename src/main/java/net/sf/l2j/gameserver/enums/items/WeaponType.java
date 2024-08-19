package net.sf.l2j.gameserver.enums.items;

import lombok.Getter;
import net.sf.l2j.gameserver.enums.skills.Stats;

@Getter
public enum WeaponType implements ItemType {
    NONE(40, null, false, false),
    SWORD(40, Stats.SWORD_WPN_VULN, true, true),
    BLUNT(40, Stats.BLUNT_WPN_VULN, true, true),
    DAGGER(40, Stats.DAGGER_WPN_VULN, true, false),
    BOW(500, Stats.BOW_WPN_VULN, false, false),
    POLE(66, Stats.POLE_WPN_VULN, false, false),
    ETC(40, null, false, false),
    FIST(40, null, false, false),
    DUAL(40, Stats.DUAL_WPN_VULN, true, false),
    DUALFIST(40, Stats.DUALFIST_WPN_VULN, true, false),
    BIGSWORD(40, Stats.BIGSWORD_WPN_VULN, true, false),
    FISHINGROD(40, null, false, false),
    BIGBLUNT(40, Stats.BIGBLUNT_WPN_VULN, true, false),
    PET(40, null, false, false);

    public static final WeaponType[] VALUES = values();

    private final int mask;

    private final int range;
    private final Stats resStat;
    private final boolean isParryWeapon;
    private final boolean isEmbraced;

    WeaponType(int range, Stats resStat, boolean isParryWeapon, boolean isEmbraced) {
        this.mask = 1 << ordinal();
        this.range = range;
        this.resStat = resStat;
        this.isParryWeapon = isParryWeapon;
        this.isEmbraced = isEmbraced;
    }

}