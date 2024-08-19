package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ConditionPlayerGrip extends Condition {

    private final boolean isUsingGrip;

    public ConditionPlayerGrip(boolean isUsingGrip) {
        this.isUsingGrip = isUsingGrip;
    }

    @Override
    public boolean testImpl(Creature effector, Creature effected, L2Skill skill, Item item) {
        return isUsingGrip && effector.getActingPlayer().getTwoHandGrip().get();
    }

}