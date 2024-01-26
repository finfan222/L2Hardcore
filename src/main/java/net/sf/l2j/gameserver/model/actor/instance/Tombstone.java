package net.sf.l2j.gameserver.model.actor.instance;

import lombok.Getter;
import lombok.Setter;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.graveyard.PostScript;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author finfan
 */
@Getter
@Setter
public class Tombstone extends Folk {

    private final PostScript data;

    public Tombstone(int objectId, NpcTemplate template, PostScript ps) {
        super(objectId, template);
        this.data = ps;
    }

    @Override
    public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed) {
        if (isCtrlPressed) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
        } else {
            super.onAction(player, false, isShiftPressed);
        }
    }

    @Override
    public void showChatWindow(Player player) {
        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile("data/html/tombstone.htm");
        html.replace("%objectId%", getObjectId());
        html.replace("%name%", getName());
        html.replace("%message%", data.getMessage());
        html.replace("%date%", data.getDate().toString());
        player.sendPacket(html);
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    @Override
    public String getName() {
        return "Надгробие";
    }

    @Override
    public String getTitle() {
        return data.getName();
    }

}
