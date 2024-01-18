package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.DialogAnswerType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class DlgAnswer extends L2GameClientPacket {

    private int messageId;
    private int answer;
    private int requesterId;

    @Override
    protected void readImpl() {
        messageId = readD();
        answer = readD();
        requesterId = readD();
    }

    @Override
    public void runImpl() {
        final Player player = getClient().getPlayer();
        if (player == null) {
            return;
        }

        try {
            if (player.isReviveRequest()) {
                DialogAnswerType.REVIVE_REQUEST.onAnswer(player, answer);
            } else if (messageId == SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId()) {
                DialogAnswerType.TELEPORT_REQUEST.onAnswer(player, answer);
            } else if (messageId == 1983 && Config.ALLOW_WEDDING) {
                DialogAnswerType.WEDDING_REQUEST.onAnswer(player, answer);
            } else if (messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId()) {
                DialogAnswerType.OPEN_GATE.onAnswer(player, answer);
            } else if (messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId()) {
                DialogAnswerType.CLOSE_GATE.onAnswer(player, answer);
            } else if (messageId == SystemMessageId.YOU_WANT_TO_LEARN_S1.getId()) {
                throw new UnsupportedOperationException("Unhandled 'DlgAnswer' message type.");
            } else if (messageId == SystemMessageId.YOU_WANT_TO_SPENT_S1_ADENA_FOR_REPAIR.getId()) {
                DialogAnswerType.REPAIR_ALL.onAnswer(player, answer);
            } else if (messageId == SystemMessageId.YOU_WANT_TO_SPENT_S1_ADENA_FOR_REPAIR_S2.getId()) {
                DialogAnswerType.REPAIR_SINGLE.onAnswer(player, answer);
            }
        } finally {
            player.setDialog(null);
        }
    }

}