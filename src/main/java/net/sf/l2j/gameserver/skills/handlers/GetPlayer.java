package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class GetPlayer extends L2Skill {

    public GetPlayer(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        for (WorldObject target : targets) {
            final Player victim = target.getActingPlayer();
            if (victim == null || victim.isAlikeDead()) {
                continue;
            }

            victim.instantTeleportTo(caster.getPosition(), 0);
        }
    }
}