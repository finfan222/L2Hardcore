package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;

import java.util.HashMap;
import java.util.Map;

public class PartyMemberPosition extends L2GameServerPacket {
    Map<Integer, Location> _locations = new HashMap<>();

    public PartyMemberPosition(Party party) {
        reuse(party);
    }

    public void reuse(Party party) {
        _locations.clear();

        for (Player member : party.getMembers()) {
            _locations.put(member.getObjectId(), member.getPosition().clone());
        }
    }

    @Override
    protected void writeImpl() {
        writeC(0xa7);
        writeD(_locations.size());

        for (Map.Entry<Integer, Location> entry : _locations.entrySet()) {
            writeD(entry.getKey());
            writeLoc(entry.getValue());
        }
    }
}