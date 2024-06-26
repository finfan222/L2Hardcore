package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.basefuncs.FuncAdd;

/**
 * @author finfan
 */
public class EffectGuard extends AbstractEffect {

    private final double pDef;
    private final double mDef;

    public EffectGuard(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
        pDef = effector.getCalculators()[Stats.POWER_DEFENCE.ordinal()].calc();
        mDef = effector.getCalculators()[Stats.MAGIC_DEFENCE.ordinal()].calc();
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.GUARD;
    }

    @Override
    public boolean onStart() {
        FuncAdd pDefAdd = new FuncAdd(this, Stats.POWER_DEFENCE, pDef, null);
        FuncAdd mDefAdd = new FuncAdd(this, Stats.MAGIC_DEFENCE, mDef, null);
        getEffected().addStatFunc(pDefAdd);
        getEffected().addStatFunc(mDefAdd);
        return super.onStart();
    }

    @Override
    public void onExit() {
        getEffected().removeStatsByOwner(this);
        super.onExit();
    }

    @Override
    public boolean onActionTime() {
        if (getEffected() == getEffector()) {
            return true;
        }

        if (getEffector().isDead() || getEffected() instanceof Player pc && !pc.isOnline()) {
            return false;
        }

        return getEffected().isIn2DRadius(getEffector(), getSkill().getSkillRadius());
    }

}
