package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.taskmanager.DayNightTaskManager;

public class ClientSetTime extends L2GameServerPacket {

    private final int time;

    public ClientSetTime(int time) {
        this.time = time;
    }

    public ClientSetTime() {
        time = DayNightTaskManager.getInstance().getDayTime();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xEC);
        writeD(time);
        writeD(0x01); // time scale divider
    }
}