package net.sf.l2j.gameserver.network.serverpackets;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ClientSetTime extends L2GameServerPacket {

    private int time;

    @Override
    protected final void writeImpl() {
        writeC(0xEC);
        writeD(time);
        writeD(0x01); // time scale divider
    }
}