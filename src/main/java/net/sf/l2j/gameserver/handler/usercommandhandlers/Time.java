package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DayNightTaskManager;

import java.time.LocalTime;

public class Time implements IUserCommandHandler {
    private static final int[] COMMAND_IDS =
        {
            77
        };

    @Override
    public void useUserCommand(int id, Player player) {
        LocalTime time = LocalTime.from(DayNightTaskManager.getInstance().getTime());
        DayCycle currentCycle = DayNightTaskManager.getInstance().getCurrentCycle();
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TIME_S1_S2_IN_S3)
            .addNumber(time.getHour())
            .addNumber(time.getMinute())
            .addString(currentCycle.getName()));
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}