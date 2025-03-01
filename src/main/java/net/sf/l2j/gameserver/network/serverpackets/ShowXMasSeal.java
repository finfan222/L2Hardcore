package net.sf.l2j.gameserver.network.serverpackets;

public class ShowXMasSeal extends L2GameServerPacket {
    private final int _item;

    public ShowXMasSeal(int item) {
        _item = item;
    }

    @Override
    protected void writeImpl() {
        writeC(0xF2);
        writeD(_item);
    }
}