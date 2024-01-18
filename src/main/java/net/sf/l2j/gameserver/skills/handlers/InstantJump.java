package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.L2Skill;

public class InstantJump extends L2Skill {

    public InstantJump(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        Creature target = (Creature) targets[0];

        int px = target.getX();
        int py = target.getY();
        double ph = MathUtil.convertHeadingToDegree(target.getHeading());

        ph += 180;

        if (ph > 360) {
            ph -= 360;
        }

        ph = (Math.PI * ph) / 180;

        int x = (int) (px + (25 * Math.cos(ph)));
        int y = (int) (py + (25 * Math.sin(ph)));
        int z = target.getZ();

        // Abort attack, cast and move.
        caster.abortAll(false);

        // Teleport the actor.
        caster.setXYZ(x, y, z);
        caster.broadcastPacket(new ValidateLocation(caster));
    }
}