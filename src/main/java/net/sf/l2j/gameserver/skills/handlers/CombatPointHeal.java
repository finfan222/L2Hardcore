package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class CombatPointHeal extends L2Skill {

    public CombatPointHeal(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        for (WorldObject obj : targets) {
            if (!(obj instanceof Player target)) {
                continue;
            }

            if (target.isDead() || target.isInvul()) {
                continue;
            }

            double cp = getPower();

            if ((target.getStatus().getCp() + cp) >= target.getStatus().getMaxCp()) {
                cp = target.getStatus().getMaxCp() - target.getStatus().getCp();
            }

            target.getStatus().setCp(cp + target.getStatus().getCp());

            if (caster != target && caster instanceof Player player) {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_CP_WILL_BE_RESTORED_BY_S1).addCharName(player).addNumber((int) cp));
            } else {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addNumber((int) cp));
            }
        }
    }
}