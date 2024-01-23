package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.ArrayList;
import java.util.List;

public class EffectConfusion extends AbstractEffect {
    public EffectConfusion(EffectTemplate template, L2Skill skill, Creature effected, Creature effector) {
        super(template, skill, effected, effector);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.CONFUSION;
    }

    @Override
    public boolean onStart() {
        // Abort move.
        getEffected().getMove().stop();

        // Refresh abnormal effects.
        getEffected().updateAbnormalEffect();

        onActionTime();
        return true;
    }

    @Override
    public void onExit() {
        getEffected().removeEffect(this);

        if (!(getEffected() instanceof Player)) {
            getEffected().getAI().notifyEvent(AiEventType.THINK, null, null);
        }

        // Refresh abnormal effects.
        getEffected().updateAbnormalEffect();
    }

    @Override
    public boolean onActionTime() {
        List<Creature> targetList = new ArrayList<>();
        getEffected().getKnownTypeInRadius(Creature.class, 1600)
            .stream()
            .filter(e -> !(e instanceof Door || e instanceof Chest))
            .forEach(targetList::add);

        // if there is no target, exit function
        if (targetList.isEmpty()) {
            return true;
        }

        // Choosing randomly a new target
        Creature target = Rnd.get(targetList);

        // Attacking the target
        getEffected().setTarget(target);
        getEffected().getAI().tryToAttack(target);

        // Add aggro to that target aswell. The aggro power is random.
        if (getEffected() instanceof Attackable attackable) {
            int aggro = (5 + Rnd.get(5)) * getEffector().getStatus().getLevel();
            attackable.getAggroList().addDamageHate(target, 0, aggro);
        }
        return true;
    }

    @Override
    public int getEffectFlags() {
        return EffectFlag.CONFUSED.getMask();
    }
}