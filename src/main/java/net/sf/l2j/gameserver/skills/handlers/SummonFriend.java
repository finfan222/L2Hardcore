package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.Collections;
import java.util.Map;

public class SummonFriend extends L2Skill {

    public SummonFriend(StatSet set) {
        super(set);
    }

    /**
     * Test if the current {@link Player} can summon. Send back messages if he can't.
     *
     * @param player : The {@link Player} to test.
     * @return True if the {@link Player} can summon, false otherwise.
     */
    public static boolean checkSummoner(Player player) {
        if (player.isMounted()) {
            return false;
        }

        if (player.isInOlympiadMode() || player.isInObserverMode() || player.isInsideZone(ZoneId.NO_SUMMON_FRIEND)) {
            player.sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
            return false;
        }
        return true;
    }

    /**
     * Test if the {@link WorldObject} can be summoned. Send back messages if he can't.
     *
     * @param player : The {@link Player} to test.
     * @param target : The {@link WorldObject} to test.
     * @return True if the given {@link WorldObject} can be summoned, false otherwise.
     */
    public static boolean checkSummoned(Player player, WorldObject target) {
        if (!(target instanceof Player targetPlayer)) {
            return false;
        }

        if (targetPlayer == player) {
            player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
            return false;
        }

        if (targetPlayer.isAlikeDead()) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addCharName(targetPlayer));
            return false;
        }

        if (targetPlayer.isOperating()) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addCharName(targetPlayer));
            return false;
        }

        if (targetPlayer.isRooted() || targetPlayer.isInCombat()) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addCharName(targetPlayer));
            return false;
        }

        if (targetPlayer.isInOlympiadMode()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
            return false;
        }

        if (targetPlayer.isFestivalParticipant() || targetPlayer.isMounted()) {
            player.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            return false;
        }

        if (targetPlayer.isInObserverMode() || targetPlayer.isInsideZone(ZoneId.NO_SUMMON_FRIEND)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IN_SUMMON_BLOCKING_AREA).addCharName(targetPlayer));
            return false;
        }
        return true;
    }

    /**
     * Teleport the current {@link Player} to the destination of another player.<br>
     * <br>
     * Check if summoning is allowed, and consume items if {@link L2Skill} got such constraints.
     *
     * @param receiver : The {@link Player} which receive teleport request.
     * @param requester : The {@link Player} to teleport on (requester).
     * @param skill : The {@link L2Skill} used to find item consumption informations.
     */
    public static void teleportTo(Player receiver, Player requester, L2Skill skill) {
        if (!checkSummoner(receiver) || !checkSummoned(receiver, requester)) {
            return;
        }

        if (skill.getTargetConsumeId() > 0 && skill.getTargetConsume() > 0) {
            if (receiver.getInventory().getItemCount(skill.getTargetConsumeId()) < skill.getTargetConsume()) {
                receiver.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING).addItemName(skill.getTargetConsumeId()));
                return;
            }

            receiver.destroyItemByItemId("Consume", skill.getTargetConsumeId(), skill.getTargetConsume(), receiver, true);
        }
        receiver.teleportTo(requester.getPosition(), 20);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        // Check player status.
        if (!checkSummoner(player)) {
            return;
        }

        // Bypass target and stuff, simply retrieve Party exclude caster.
        if (getSkillType() == SkillType.SUMMON_PARTY) {
            final Party party = player.getParty();
            if (party == null) {
                return;
            }

            for (Player member : party.getMembers()) {
                if (member == player) {
                    continue;
                }

                // Check target status.
                if (!checkSummoned(player, member)) {
                    continue;
                }

                teleportTo(member, player, this);
            }
        } else {
            for (WorldObject obj : targets) {
                // The target must be a player.
                if (!(obj instanceof Player target)) {
                    continue;
                }

                // Check target status.
                if (!checkSummoned(player, target)) {
                    continue;
                }

                // Check target teleport request status.
                if (target.hasDialog(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT)) {
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_SUMMONED).addCharName(target));
                    continue;
                }

                // Send a request for Summon Friend skill.
                if (getId() == 1403) {
                    final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
                    confirm.addCharName(player);
                    confirm.addZoneName(caster.getPosition());
                    confirm.addTime(15000);
                    confirm.addRequesterId(player.getObjectId());
                    target.setDialog(new Dialog(target, confirm, Map.of("skill", this)).send());
                } else {
                    teleportTo(target, player, this);
                }
            }
        }
    }
}