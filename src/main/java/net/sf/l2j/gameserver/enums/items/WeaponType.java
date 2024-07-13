package net.sf.l2j.gameserver.enums.items;

import net.sf.l2j.gameserver.enums.skills.Stats;

public enum WeaponType implements ItemType {
    NONE(40, null, false),
    SWORD(40, Stats.SWORD_WPN_VULN, true),
    BLUNT(40, Stats.BLUNT_WPN_VULN, true),
    DAGGER(40, Stats.DAGGER_WPN_VULN, true),
    BOW(500, Stats.BOW_WPN_VULN, false),
    POLE(66, Stats.POLE_WPN_VULN, false),
    ETC(40, null, false),
    FIST(40, null, false),
    DUAL(40, Stats.DUAL_WPN_VULN, true),
    DUALFIST(40, Stats.DUALFIST_WPN_VULN, true),
    BIGSWORD(40, Stats.BIGSWORD_WPN_VULN, true),
    FISHINGROD(40, null, false),
    BIGBLUNT(40, Stats.BIGBLUNT_WPN_VULN, true),
    PET(40, null, false);

    public static final WeaponType[] VALUES = values();

    private final int _mask;

    private final int _range;
    private final Stats _vulnStat;
    private final boolean _parryWeapon;

    WeaponType(int range, Stats stat, boolean parryWeapon) {
        _mask = 1 << ordinal();

        _range = range;
        _vulnStat = stat;
        _parryWeapon = parryWeapon;
    }

    @Override
    public int getMask() {
        return _mask;
    }

    public int getRange() {
        return _range;
    }

    public Stats getVulnStat() {
        return _vulnStat;
    }

    public boolean isParryWeapon() {
        return _parryWeapon;
    }
}