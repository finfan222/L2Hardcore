package net.sf.l2j.gameserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

import java.util.Map;

/**
 * @author finfan
 */
@Data
@AllArgsConstructor
public class Dialog {

    private final Player player;
    private final ConfirmDlg packet;
    private final Map<String, Object> context;

    public Dialog send() {
        player.sendPacket(packet);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T findAndGet(String key) {
        return (T) context.get(key);
    }

    public boolean isMessage(SystemMessageId messageId) {
        return packet.getMessageId() == messageId.getId();
    }

    public boolean isMessage(int id) {
        return packet.getMessageId() == id;
    }

}
