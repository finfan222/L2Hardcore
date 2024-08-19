package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import lombok.Getter;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageColor;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author finfan
 */
public class EquipListener implements OnEquipListener {

    @Getter(lazy = true)
    private static final EquipListener instance = new EquipListener();

    @Override
    public void onEquip(Paperdoll slot, ItemInstance item, Playable actor) {
        if (actor instanceof Player player) {
            if (item.getWeaponItem() != null) {
                player.setAttackType(item.getWeaponItem().getItemType());
            } else {
                player.setAttackType(null);
            }
        }
    }

    @Override
    public void onUnequip(Paperdoll slot, ItemInstance item, Playable actor) {
        if (actor instanceof Player player) {
            if (player.getTwoHandGrip().compareAndSet(true, false)) {
                if (player.getLeftHand() != null) {
                    player.getInventory().equipItem(player.getLeftHand());
                    InventoryUpdate update = new InventoryUpdate();
                    update.addModifiedItem(player.getLeftHand());
                    player.sendPacket(update);
                    player.setLeftHand(null);
                }
                player.setAttackType(null);
                player.sendMessage("Двуручный хват отменен.", SystemMessageColor.RED_LIGHT);
            }
        }
    }

}
