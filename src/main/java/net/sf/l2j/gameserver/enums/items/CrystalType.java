package net.sf.l2j.gameserver.enums.items;

import lombok.Getter;

@Getter
public enum CrystalType {
    NONE(0, 0, 0, 0, 6),
    D(1, 1458, 11, 90, 4),
    C(2, 1459, 6, 45, 6),
    B(3, 1460, 11, 67, 6),
    A(4, 1461, 19, 144, 4),
    S(5, 1462, 25, 250, 8);

    private final int id;
    private final int crystalId;
    private final int crystalEnchantBonusArmor;
    private final int crystalEnchantBonusWeapon;
    private final int repairModifier;

    CrystalType(int id, int crystalId, int crystalEnchantBonusArmor, int crystalEnchantBonusWeapon, int repairModifier) {
        this.id = id;
        this.crystalId = crystalId;
        this.crystalEnchantBonusArmor = crystalEnchantBonusArmor;
        this.crystalEnchantBonusWeapon = crystalEnchantBonusWeapon;
        this.repairModifier = repairModifier;
    }

    public boolean isGreater(CrystalType crystalType) {
        return getId() > crystalType.getId();
    }

    public boolean isLesser(CrystalType crystalType) {
        return getId() < crystalType.getId();
    }
}