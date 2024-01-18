package net.sf.l2j.gameserver.enums.items;

import lombok.Getter;

@Getter
public enum ArmorType implements ItemType {
    NONE(1, 1),
    LIGHT(0.66, 1.5),
    HEAVY(1, 1),
    MAGIC(0.33, 2.0),
    PET(1, 1),
    SHIELD(4, 0.33);

    public static final ArmorType[] VALUES = values();

    private final double damageToWeaponDurability;
    private final double durabilityAbsorb;
    private final int mask;

    ArmorType(double damageToWeaponDurability, double durabilityAbsorb) {
        this.damageToWeaponDurability = damageToWeaponDurability;
        this.durabilityAbsorb = durabilityAbsorb;
        this.mask = 1 << (ordinal() + WeaponType.values().length);
    }

}