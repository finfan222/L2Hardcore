package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class TakeCastle extends L2Skill {

    public TakeCastle(StatSet set) {
        super(set);
    }

    /**
     * @param player : The {@link Player} to test.
     * @param target : The {@link WorldObject} to test.
     * @param skill : The {@link L2Skill} to test.
     * @param announce : If True, broadcast to SiegeSide.DEFENDER than opponents started to engrave.
     * @return The {@link Castle} affiliated to the {@link WorldObject} target, or null if operation aborted for a
     * condition or another.
     */
    public static Castle check(Player player, WorldObject target, L2Skill skill, boolean announce) {
        final Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle == null || castle.getCastleId() <= 0) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
        } else if (!castle.isGoodArtifact(target)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET));
        } else if (!castle.getSiege().isInProgress()) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
        } else if (!player.isIn3DRadius(target, 200)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
        } else if (!player.isInsideZone(ZoneId.CAST_ON_ARTIFACT)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
        } else if (!castle.getSiege().checkSide(player.getClan(), SiegeSide.ATTACKER)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
        } else {
            if (announce) {
                castle.getSiege().announce(SystemMessageId.OPPONENT_STARTED_ENGRAVING, SiegeSide.DEFENDER);
            }

            return castle;
        }
        return null;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        if (targets.length == 0) {
            return;
        }

        if (!player.isClanLeader()) {
            return;
        }

        final Castle castle = check(player, targets[0], this, false);
        if (castle == null) {
            return;
        }

        castle.engrave(player.getClan(), targets[0]);
    }
}