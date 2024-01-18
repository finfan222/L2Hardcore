package net.sf.l2j.gameserver.model.actor.attack;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * This class groups all attack data related to a {@link Creature}.
 */
public class PlayerAttack extends PlayableAttack<Player> {
    public PlayerAttack(Player creature) {
        super(creature);
    }

    @Override
    public boolean doAttack(Creature target) {
        final boolean isHit = super.doAttack(target);
        if (isHit) {
            // If hit by a CW or by an hero while holding a CW, CP are reduced to 0.
            if (target instanceof Player && !target.isInvul()) {
                final Player targetPlayer = (Player) target;
                if (attacker.isCursedWeaponEquipped() || (attacker.isHero() && targetPlayer.isCursedWeaponEquipped())) {
                    targetPlayer.getStatus().setCp(0);
                }
            }
        }

        attacker.clearRecentFakeDeath();
        return isHit;
    }

    @Override
    public boolean canDoAttack(Creature target) {
        if (!super.canDoAttack(target)) {
            return false;
        }

        final Weapon weaponItem = attacker.getActiveWeaponItem();

        switch (weaponItem.getItemType()) {
            case FISHINGROD:
                attacker.sendPacket(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE);
                return false;

            case BOW:
                if (!attacker.checkAndEquipArrows()) {
                    attacker.sendPacket(SystemMessageId.NOT_ENOUGH_ARROWS);
                    return false;
                }

                final int mpConsume = weaponItem.getMpConsume();
                if (mpConsume > 0 && mpConsume > attacker.getStatus().getMp()) {
                    attacker.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
                    return false;
                }
        }
        return true;
    }
}