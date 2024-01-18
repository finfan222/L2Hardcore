package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

public class GiveSp extends L2Skill {

    public GiveSp(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final int spToAdd = (int) getPower();
        for (WorldObject obj : targets) {
            final Creature target = (Creature) obj;
            if (target != null) {
                target.addExpAndSp(0, spToAdd);
            }
        }
    }
}