package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.enums.SocialType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

public class SocialAction extends L2GameServerPacket {

    private final int _charObjId;
    private final int _actionId;

    public SocialAction(Creature cha, int actionId) {
        _charObjId = cha.getObjectId();
        _actionId = actionId;
    }

    public SocialAction(Player player, SocialType type) {
        _charObjId = player.getObjectId();
        _actionId = type.getId();
    }

    @Override
    protected final void writeImpl() {
        writeC(0x2d);
        writeD(_charObjId);
        writeD(_actionId);
    }
}