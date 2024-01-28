package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

public final class RequestDestroyItem extends L2GameClientPacket {

    private int _objectId;
    private int _count;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _count = readD();
    }

    @Override
    protected void runImpl() {
        final Player player = getClient().getPlayer();
        if (player == null) {
            return;
        }

        if (player.isProcessingTransaction() || player.isOperating()) {
            player.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }

        final ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);
        if (itemToRemove == null) {
            return;
        }

        if (_count < 1 || _count > itemToRemove.getCount()) {
            player.sendPacket(SystemMessageId.CANNOT_DESTROY_NUMBER_INCORRECT);
            return;
        }

        if (!itemToRemove.isStackable() && _count > 1) {
            return;
        }

        final int itemId = itemToRemove.getItemId();
        if (!itemToRemove.isDestroyable() || CursedWeaponManager.getInstance().isCursed(itemId)) {
            player.sendPacket((itemToRemove.isHeroItem()) ? SystemMessageId.HERO_WEAPONS_CANT_DESTROYED : SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
            return;
        }

        if (itemToRemove.isEquipped() && (!itemToRemove.isStackable() || (itemToRemove.isStackable() && _count >= itemToRemove.getCount()))) {
            final ItemInstance[] unequipped = player.getInventory().unequipItemInSlotAndRecord(itemToRemove.getSlot());
            final InventoryUpdate iu = new InventoryUpdate();
            for (ItemInstance item : unequipped) {
                item.unChargeAllShots();
                iu.addModifiedItem(item);
            }

            player.sendPacket(iu);
            player.broadcastUserInfo();
        }

        // if it's a pet control item.
        if (itemToRemove.isSummonItem()) {
            // See if pet or mount is active ; can't destroy item linked to that pet.
            if ((player.getSummon() != null && player.getSummon().getControlItemObjectId() == _objectId) || (player.isMounted() && player.getMountObjectId() == _objectId)) {
                player.sendPacket(SystemMessageId.PET_SUMMONED_MAY_NOT_DESTROYED);
                return;
            }
        }

        player.destroyItem("Destroy", _objectId, _count, player, true);
    }
}