package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

public class DrainSoul extends L2Skill {

    public DrainSoul(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        // Check player.
        if (caster == null || caster.isDead() || !(caster instanceof Player player)) {
            return;
        }

        // Check quest condition.
        QuestState st = player.getQuestList().getQuestState("Q350_EnhanceYourWeapon");
        if (st == null || !st.isStarted()) {
            return;
        }

        // Get target.
        WorldObject target = targets[0];
        if (!(target instanceof Monster mob)) {
            return;
        }

        // Check monster.
        if (mob.isDead()) {
            return;
        }

        // Range condition, cannot be higher than skill's effectRange.
        if (!player.isIn3DRadius(mob, getEffectRange())) {
            return;
        }

        // Register.
        mob.registerAbsorber(player);
    }
}