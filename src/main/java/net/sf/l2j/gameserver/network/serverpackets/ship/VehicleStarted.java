package net.sf.l2j.gameserver.network.serverpackets.ship;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public class VehicleStarted extends L2GameServerPacket {
    private final int _objectId;
    private final int _state;

    public VehicleStarted(Creature boat, int state) {
        _objectId = boat.getObjectId();
        _state = state;
    }

    @Override
    protected void writeImpl() {
        writeC(0xBA);
        writeD(_objectId);
        writeD(_state);
    }
}