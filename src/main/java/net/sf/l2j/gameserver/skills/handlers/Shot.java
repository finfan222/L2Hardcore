package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;

/**
 * @author finfan
 */
public class Shot extends Default {

    public Shot(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
    }

}
