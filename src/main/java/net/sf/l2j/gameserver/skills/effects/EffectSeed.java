package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EffectSeed extends AbstractEffect {
    private int _power = 1;

    public EffectSeed(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.SEED;
    }

    @Override
    public boolean onActionTime() {
        return false;
    }

    public int getPower() {
        return _power;
    }

    public void increasePower() {
        _power++;
    }
}