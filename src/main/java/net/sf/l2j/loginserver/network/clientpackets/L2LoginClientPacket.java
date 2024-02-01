package net.sf.l2j.loginserver.network.clientpackets;

import net.sf.l2j.commons.mmocore.ReceivablePacket;
import net.sf.l2j.loginserver.network.LoginClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class L2LoginClientPacket extends ReceivablePacket<LoginClient> {
    protected static final Logger log = LoggerFactory.getLogger(L2LoginClientPacket.class.getName());

    @Override
    protected final boolean read() {
        try {
            return readImpl();
        } catch (Exception e) {
            log.error("Failed reading {}. ", getClass().getSimpleName(), e);
            return false;
        }
    }

    protected abstract boolean readImpl();
}
