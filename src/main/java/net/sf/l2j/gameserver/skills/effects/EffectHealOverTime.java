package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExRegenMax;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EffectHealOverTime extends AbstractEffect {
    public EffectHealOverTime(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.HEAL_OVER_TIME;
    }

    @Override
    public boolean onStart() {
        // If effected is a player, send a hp regen effect packet.
        if (getEffected() instanceof Player && getTemplate().getCounter() > 0 && getPeriod() > 0) {
            getEffected().sendPacket(new ExRegenMax(getTemplate().getCounter() * getPeriod(), getPeriod(), getTemplate().getValue()));
        }

        return true;
    }

    @Override
    public boolean onActionTime() {
        if (!getEffected().canBeHealed()) {
            return false;
        }

        double value = getTemplate().getValue();
        if (getEffected().isAffected(EffectFlag.POISON)) {
            value *= 1 - Rnd.get(0, 30) / 100.;
        }
        getEffected().getStatus().addHp(value);
        return true;
    }
}