package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.modules.DurabilityModule;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ChooseInventoryItem;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class handles skills trainers.
 */
public final class Trainer extends Folk {

    private static final String[] REPAIR_TEXT = {
        """
        Ah, greetings, <font color=LEVEL>adventurer</font>! Indeed, you have come to the right place. I am the esteemed 
        provider of repair services for all your weapons and armor needs. If you have any worn-out or damaged equipment, 
        fear not! I possess the skills and knowledge to restore them to their former glory.
        """,
        """
        Greetings, <font color=LEVEL>adventurer</font>! You've come to the right place. I specialize in repairing\s
        weapons and armor. Whether you need a sword sharpened or a set of plate armor fixed, I can take care of it. 
        Simply give me your worn-out equipment, and I'll have it restored to its former glory in no time.
        """,
        """
        Ah, greetings, <font color=LEVEL>adventurer</font>! Indeed, you've come to the right place. I am the expert in 
        all things related to weapon and armor repairs. No matter how battered or shattered your gear may be, I can 
        mend it for you!"
        """
    };

    public Trainer(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        String filename = "";
        if (val == 0) {
            filename = "" + npcId;
        } else {
            filename = npcId + "-" + val;
        }

        return "data/html/trainer/" + filename + ".htm";
    }

    @Override
    public void onBypassFeedback(Player player, String command) {
        if (command.startsWith("exc_repair_")) {
            Set<ItemInstance> items = player.getInventory().getAllBrokenItems();
            if (command.endsWith("list")) {
                if (items.isEmpty()) {
                    player.sendPacket(SystemMessageId.CANT_REPAIR_ALL_ITEMS_ARE_GOOD);
                    return;
                }

                player.setActiveEnchantItem(ItemData.DUMMY);
                player.sendPacket(new ChooseInventoryItem(0));
            } else if (command.endsWith("all")) {
                payForRepair(player, items);
            } else {
                throw new UnsupportedOperationException(String.format("Command %s not implemented for npcId=%d.", command, getNpcId()));
            }
        } else if (command.startsWith("Link")) {
            final String path = command.substring(5).trim();
            if (path.contains("..")) {
                return;
            }

            final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/" + path);
            html.replace("%objectId%", getObjectId());
            html.replace("%npcName%", getName());
            html.replace("%repair_answer%", REPAIR_TEXT[Rnd.get(REPAIR_TEXT.length - 1)]);
            player.sendPacket(html);
        } else {
            super.onBypassFeedback(player, command);
        }

    }

    private void payForRepair(Player player, Set<ItemInstance> items) {
        if (player.isInDuel()
            || AttackStanceTaskManager.getInstance().isInAttackStance(player)
            || player.isAlikeDead()
            || player.isInOlympiadMode()
            || player.getRequest().isProcessingRequest()) {
            player.sendPacket(SystemMessageId.ACCESS_FAILED);
            return;
        }

        int adena = player.getAdena();
        AtomicInteger price = new AtomicInteger(0);

        if (items.isEmpty()) {
            player.sendPacket(SystemMessageId.CANT_REPAIR_ALL_ITEMS_ARE_GOOD);
            return;
        }

        for (ItemInstance item : items) {
            Optional.ofNullable(item.getModule(DurabilityModule.class))
                .ifPresent(e -> price.addAndGet(e.getRepairPrice()));
        }

        if (adena < price.get()) {
            player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            return;
        }

        ConfirmDlg packet = new ConfirmDlg(SystemMessageId.YOU_WANT_TO_SPENT_S1_ADENA_FOR_REPAIR);
        packet.addTime(15000);
        packet.addNumber(price.get());
        player.setDialog(new Dialog(player, packet, Map.of("items", items, "price", price.get())).send());
    }
}