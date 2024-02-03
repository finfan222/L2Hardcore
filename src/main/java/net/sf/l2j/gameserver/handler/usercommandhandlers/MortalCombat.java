package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.data.manager.DuelManager;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public class MortalCombat implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = {
        114
    };

    @Override
    public void useUserCommand(int id, Player player) {
        if (player.getTarget() instanceof Player opponent) {
            DuelManager.getInstance().requestMortalCombat(player, opponent);
        } else {
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}