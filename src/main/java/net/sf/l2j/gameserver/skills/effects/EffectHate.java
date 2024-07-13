package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EffectHate extends EffectTargetMe {

    private final boolean abortCast;

    public EffectHate(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
        abortCast = template.getParams().getBool("abortCast");
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.HATE;
    }

    @Override
    public boolean onStart() {
        super.onStart();

        if (abortCast && getEffected().getCast().isCastingNow()) {
            getEffected().getCast().interrupt();
        }

        getEffected().getUncontrolled().set(true);
        return true;
    }

    @Override
    public void onExit() {
        getEffected().getUncontrolled().set(false);
    }
}