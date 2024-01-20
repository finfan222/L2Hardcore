package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EffectPoison extends EffectDamOverTime {

    public EffectPoison(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.POISON;
    }

    @Override
    public boolean onActionTime() {
        if (getEffected().isDead()) {
            return false;
        }

        double damage = getEffected().getStatus().calcStat(Stats.POISON_VULN, getTemplate().getValue(), getEffected(), _skill);
        getEffected().reduceCurrentHpByDOT(damage, getEffector(), getSkill());

        double poisonResistance = getEffected().getStatus().calcStat(Stats.POISON_VULN, 0, getEffected(), _skill);
        System.out.println("[DEBUG] Poison resist: " + poisonResistance);
        if (Rnd.calcChance(poisonResistance / 4, 100)) {
            exit();
            return false;
        }
        return true;
    }

    @Override
    public int getEffectFlags() {
        return EffectFlag.POISON.getMask();
    }
}