package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.commons.mmocore.SendablePacket;
import net.sf.l2j.gameserver.network.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class L2GameServerPacket extends SendablePacket<GameClient> {
    protected static final Logger log = LoggerFactory.getLogger(L2GameServerPacket.class.getName());

    protected abstract void writeImpl();

    @Override
    protected void write() {
        if (Config.PACKET_HANDLER_DEBUG) {
            log.info(getType());
        }

        try {
            writeImpl();
        } catch (Exception e) {
            log.error("Failed writing {} for {}. ", getType(), getClient().toString(), e);
        }
    }

    public void runImpl() {
    }

    public String getType() {
        return "[S] " + getClass().getSimpleName();
    }
}