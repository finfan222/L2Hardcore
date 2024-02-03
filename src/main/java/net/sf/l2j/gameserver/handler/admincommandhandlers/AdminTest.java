package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ClientSetTime;

import java.util.StringTokenizer;

public class AdminTest implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
        {
            "admin_test",
        };

    @Override
    public void useAdminCommand(String command, Player player) {
        final StringTokenizer st = new StringTokenizer(command);
        st.nextToken();

        if (!st.hasMoreTokens()) {
            player.sendMessage("Usage : //test ...");
            return;
        }

        switch (st.nextToken()) {
            // Add your own cases.
            case "time":
                for (int i = 0; i < 144; i++) {
                    int value = (i + 1) * 10;
                    player.sendPacket(new ClientSetTime(value));
                    log.info("> {}", value);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            case "times":
                player.sendPacket(new ClientSetTime(Integer.valueOf(st.nextToken())));
                break;
            default:
                player.sendMessage("Usage : //test ...");
                break;
        }
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}