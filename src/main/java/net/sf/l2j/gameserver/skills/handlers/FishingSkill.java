package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.skills.L2Skill;

public class FishingSkill extends L2Skill {

    public FishingSkill(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        final boolean isReelingSkill = getSkillType() == SkillType.REELING;

        if (!player.getFishingStance().isUnderFishCombat()) {
            player.sendPacket((isReelingSkill) ? SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING : SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        final ItemInstance fishingRod = caster.getActiveWeaponInstance();
        if (fishingRod == null || fishingRod.getItem().getItemType() != WeaponType.FISHINGROD) {
            return;
        }

        final int ssBonus = (caster.isChargedShot(ShotType.FISH_SOULSHOT)) ? 2 : 1;
        final double gradeBonus = 1 + fishingRod.getItem().getCrystalType().getId() * 0.1;

        int damage = (int) (getPower() * gradeBonus * ssBonus);
        int penalty = 0;

        // Fish expertise penalty if skill level is superior or equal to 3.
        if (getLevel() - player.getSkillLevel(1315) >= 3) {
            penalty = 50;
            damage -= penalty;

            player.sendPacket(SystemMessageId.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY);
        }

        if (ssBonus > 1) {
            fishingRod.setChargedShot(ShotType.FISH_SOULSHOT, false);
        }

        if (isReelingSkill) {
            player.getFishingStance().useRealing(damage, penalty);
        } else {
            player.getFishingStance().usePomping(damage, penalty);
        }
    }
}