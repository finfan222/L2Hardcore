package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.DayNightTaskManager;

public class ConditionGameTime extends Condition {
    private final DayCycle dayCycle;

    public ConditionGameTime(DayCycle dayCycle) {
        this.dayCycle = dayCycle;
    }

    @Override
    public boolean testImpl(Creature effector, Creature effected, L2Skill skill, Item item) {
        return DayNightTaskManager.getInstance().is(dayCycle);
    }
}