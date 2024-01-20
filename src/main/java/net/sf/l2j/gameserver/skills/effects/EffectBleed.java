package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EffectBleed extends EffectDamOverTime {
    public EffectBleed(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.BLEED;
    }

    @Override
    public boolean onActionTime() {
        if (getEffected().isDead()) {
            return false;
        }

        double damage = getEffected().getStatus().calcStat(Stats.BLEED_VULN, getTemplate().getValue(), getEffected(), _skill);
        if (getEffected().isRunning()) {
            damage *= Rnd.get(60, 100) / 100. + 1;
        } else if (getEffected().isActing()) {
            damage *= Rnd.get(30, 50) / 100. + 1;
        }
        getEffected().reduceCurrentHpByDOT(damage, getEffector(), getSkill());

        double bleedResistance = getEffected().getStatus().calcStat(Stats.BLEED_VULN, 0, getEffected(), _skill);
        System.out.println("[DEBUG] Bleed resist: " + bleedResistance);
        if (Rnd.calcChance(bleedResistance / 4, 100)) {
            exit();
            return false;
        }
        return true;
    }

    @Override
    public int getEffectFlags() {
        return EffectFlag.BLEED.getMask();
    }

}