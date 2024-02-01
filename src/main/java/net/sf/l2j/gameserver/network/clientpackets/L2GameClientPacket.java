package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.commons.mmocore.ReceivablePacket;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;

/**
 * Packets received by the gameserver from clients.
 */
public abstract class L2GameClientPacket extends ReceivablePacket<GameClient> {

    protected static final Logger log = LoggerFactory.getLogger(L2GameClientPacket.class.getSimpleName());

    protected abstract void readImpl();

    protected abstract void runImpl();

    @Override
    protected boolean read() {
        if (Config.PACKET_HANDLER_DEBUG) {
            log.info(getType());
        }

        try {
            readImpl();
            return true;
        } catch (Exception e) {
            if (e instanceof BufferUnderflowException) {
                getClient().onBufferUnderflow();
                return false;
            }
            log.error("Failed reading {} for {}. ", getType(), getClient().toString(), e);
        }
        return false;
    }

    @Override
    public void run() {
        try {
            runImpl();

            // Depending of the packet send, removes spawn protection
            if (triggersOnActionRequest()) {
                final Player player = getClient().getPlayer();
                if (player != null && player.isSpawnProtected()) {
                    player.onActionRequest();
                }
            }
        } catch (Exception e) {
            log.error("Failed reading {} for {}. ", e, getType(), getClient().toString());

            if (this instanceof EnterWorld) {
                getClient().closeNow();
            }
        }
    }

    protected final void sendPacket(L2GameServerPacket gsp) {
        getClient().sendPacket(gsp);
    }

    /**
     * @return A String with this packet name for debuging purposes
     */
    public String getType() {
        return "[C] " + getClass().getSimpleName();
    }

    /**
     * Overriden with true value on some packets that should disable spawn protection
     *
     * @return
     */
    protected boolean triggersOnActionRequest() {
        return true;
    }
}