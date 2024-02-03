package net.sf.l2j.gameserver.model.itemcontainer;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemDao;
import net.sf.l2j.gameserver.model.item.instance.ItemFactory;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public abstract class ItemContainer {

    protected final Set<ItemInstance> _items = new ConcurrentSkipListSet<>();

    protected ItemContainer() {
    }

    protected abstract Creature getOwner();

    protected abstract ItemLocation getBaseLocation();

    public String getName() {
        return "ItemContainer";
    }

    /**
     * @return the owner objectId of the inventory.
     */
    public int getOwnerId() {
        return (getOwner() == null) ? 0 : getOwner().getObjectId();
    }

    /**
     * @return the quantity of items in the inventory.
     */
    public int getSize() {
        return _items.size();
    }

    /**
     * @return the list of items in inventory.
     */
    public Set<ItemInstance> getItems() {
        return _items;
    }

    /**
     * @param itemId : The item ID to check.
     * @return True if the item id exists in this {@link ItemContainer}, false otherwise.
     */
    public boolean hasItems(int itemId) {
        return getItemByItemId(itemId) != null;
    }

    /**
     * @param itemIds : A list of item IDs to check.
     * @return True if all item ids exist in this {@link ItemContainer}, false otherwise.
     */
    public boolean hasItems(int... itemIds) {
        for (int itemId : itemIds) {
            if (getItemByItemId(itemId) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param itemIds : A list of item IDs to check.
     * @return True if at least one item id exists in this {@link ItemContainer}, false otherwise.
     */
    public boolean hasAtLeastOneItem(int... itemIds) {
        for (int itemId : itemIds) {
            if (getItemByItemId(itemId) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param itemId : The item ID to check.
     * @return A {@link List} of {@link ItemInstance}s by given item ID, or an empty {@link List} if none are found.
     */
    public List<ItemInstance> getItemsByItemId(int itemId) {
        List<ItemInstance> temp = new ArrayList<>();
        for (ItemInstance next : _items) {
            if (next.getItemId() == itemId) {
                temp.add(next);
            }
        }
        return temp;
    }

    /**
     * @param itemId : The item ID to check.
     * @return An {@link ItemInstance} using its item ID, or null if not found in this {@link ItemContainer}.
     */
    public ItemInstance getItemByItemId(int itemId) {
        for (ItemInstance next : _items) {
            if (next.getItemId() == itemId) {
                return next;
            }
        }
        return null;
    }

    /**
     * @param objectId : The object ID to check.
     * @return An {@link ItemInstance} using its object ID, or null if not found in this {@link ItemContainer}.
     */
    public ItemInstance getItemByObjectId(int objectId) {
        for (ItemInstance next : _items) {
            if (next.getObjectId() == objectId) {
                return next;
            }
        }
        return null;
    }

    /**
     * @param itemId : The item ID to check.
     * @return The quantity of items hold by this {@link ItemContainer} (item enchant level does not matter, including
     * equipped items).
     */
    public int getItemCount(int itemId) {
        return getItemCount(itemId, -1, true);
    }

    /**
     * @param itemId : The item ID to check.
     * @param enchantLevel : The enchant level to match on (-1 for ANY enchant level).
     * @return The quantity of items hold by this {@link ItemContainer} (including equipped items).
     */
    public int getItemCount(int itemId, int enchantLevel) {
        return getItemCount(itemId, enchantLevel, true);
    }

    /**
     * @param itemId : The item ID to check.
     * @param enchantLevel : The enchant level to match on (-1 for ANY enchant level).
     * @param includeEquipped : Include equipped items.
     * @return The quantity of items hold by this {@link ItemContainer}.
     */
    public int getItemCount(int itemId, int enchantLevel, boolean includeEquipped) {
        int count = 0;

        for (ItemInstance item : _items) {
            if (item.getItemId() == itemId
                && (item.getData().getEnchantLevel() == enchantLevel || enchantLevel < 0)
                && (includeEquipped || !item.isEquipped())) {
                if (item.isStackable()) {
                    return item.getCount();
                }

                count++;
            }
        }
        return count;
    }

    /**
     * Adds item to inventory
     *
     * @param process : String identifier of process triggering this action.
     * @param item : ItemInstance to add.
     * @param actor : The player requesting the item addition.
     * @param reference : The WorldObject referencing current action (like NPC selling item or previous item in
     * transformation,...)
     * @return the ItemInstance corresponding to the new or updated item.
     */
    public ItemInstance addItem(String process, ItemInstance item, Player actor, WorldObject reference) {
        ItemInstance inventoryItem = getItemByItemId(item.getItemId());

        // If stackable item is found in inventory just add to current quantity
        if (inventoryItem != null && inventoryItem.isStackable()) {
            int count = item.getCount();
            inventoryItem.changeCount(process, count, actor, reference);
            inventoryItem.setLastChange(ItemState.MODIFIED);
            ItemDao.updateCount(inventoryItem);

            // And destroys the item
            item.destroyMe(process, actor, reference);
            ItemDao.remove(item.getObjectId());
        }
        // If item hasn't be found in inventory, create new one
        else {
            // Updates database
            item.setOwnerId(process, getOwnerId(), actor, reference);
            item.setLocation(getBaseLocation());
            item.setLastChange(ItemState.ADDED);

            // Add item in inventory
            addItem(item);

            // Update database
            if (item.getData().isExistsInDB()) {
                ItemDao.update(item);
            } else {
                ItemDao.create(item);
            }
        }

        refreshWeight();
        return item;
    }

    /**
     * Adds an item to inventory.
     *
     * @param process : String identifier of process triggering this action.
     * @param itemId : The itemId of the ItemInstance to add.
     * @param count : The quantity of items to add.
     * @param actor : The player requesting the item addition.
     * @param reference : The WorldObject referencing current action (like NPC selling item or previous item in
     * transformation,...)
     * @return the ItemInstance corresponding to the new or updated item.
     */
    public ItemInstance addItem(String process, int itemId, int count, Player actor, WorldObject reference) {
        ItemInstance item = getItemByItemId(itemId);

        // If stackable item is found in inventory just add to current quantity
        if (item != null && item.isStackable()) {
            item.changeCount(process, count, actor, reference);
            item.setLastChange(ItemState.MODIFIED);
            ItemDao.updateCount(item);
        }
        // If item hasn't be found in inventory, create new one
        else {
            final Item template = ItemData.getInstance().getTemplate(itemId);
            if (template == null) {
                return null;
            }

            for (int i = 0; i < count; i++) {
                item = ItemFactory.create(itemId, count, actor, reference);
                item.getData().setOwnerId(getOwnerId());
                item.setLocation(getBaseLocation());
                item.setLastChange(ItemState.ADDED);
                addItem(item);

                if (item.getData().isExistsInDB()) {
                    ItemDao.update(item);
                } else {
                    ItemDao.create(item);
                }

                // If stackable, end loop as entire count is included in 1 instance of item
                if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP) {
                    break;
                }
            }
        }

        refreshWeight();
        return item;
    }

    /**
     * Transfers item to another inventory
     *
     * @param process : String Identifier of process triggering this action
     * @param objectId : int objectid of the item to be transfered
     * @param count : int Quantity of items to be transfered
     * @param target
     * @param actor : Player Player requesting the item transfer
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the new item or the updated item in inventory
     */
    public ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, Player actor, WorldObject reference) {
        if (target == null) {
            return null;
        }

        ItemInstance sourceitem = getItemByObjectId(objectId);
        if (sourceitem == null) {
            return null;
        }

        ItemInstance targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getItemId()) : null;

        synchronized (sourceitem) {
            // check if this item still present in this container
            if (getItemByObjectId(objectId) != sourceitem) {
                return null;
            }

            // Check if requested quantity is available
            if (count > sourceitem.getCount()) {
                count = sourceitem.getCount();
            }

            // If possible, move entire item object
            if (sourceitem.getCount() == count && targetitem == null) {
                removeItem(sourceitem);
                target.addItem(process, sourceitem, actor, reference);
                targetitem = sourceitem;
            } else {
                if (sourceitem.getCount() > count) // If possible, only update counts
                {
                    sourceitem.changeCount(process, -count, actor, reference);
                } else
                // Otherwise destroy old item
                {
                    removeItem(sourceitem);
                    sourceitem.destroyMe(process, actor, reference);
                }

                if (targetitem != null) // If possible, only update counts
                {
                    targetitem.changeCount(process, count, actor, reference);
                } else
                // Otherwise add new item
                {
                    targetitem = target.addItem(process, sourceitem.getItemId(), count, actor, reference);
                }
            }

            // Updates database
            if (sourceitem.getLastChange() == ItemState.ADDED) {
                ItemDao.create(sourceitem);
            } else {
                ItemDao.update(sourceitem);
            }

            if (targetitem != sourceitem && targetitem != null) {
                if (targetitem.getLastChange() == ItemState.ADDED) {
                    ItemDao.create(targetitem);
                } else {
                    ItemDao.update(targetitem);
                }
            }

            if (sourceitem.isAugmented()) {
                sourceitem.getAugmentation().removeBonus(actor);
            }

            refreshWeight();
            target.refreshWeight();
        }
        return targetitem;
    }

    /**
     * Destroy item from inventory and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param item : ItemInstance to be destroyed
     * @param actor : Player Player requesting the item destroy
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public ItemInstance destroyItem(String process, ItemInstance item, Player actor, WorldObject reference) {
        return destroyItem(process, item, item.getCount(), actor, reference);
    }

    /**
     * Destroy item from inventory and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param item : ItemInstance to be destroyed
     * @param count
     * @param actor : Player Player requesting the item destroy
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public ItemInstance destroyItem(String process, ItemInstance item, int count, Player actor, WorldObject reference) {
        synchronized (item) {
            // Adjust item quantity
            if (item.getCount() > count) {
                item.changeCount(process, -count, actor, reference);
                item.setLastChange(ItemState.MODIFIED);

                // don't update often for untraced items
                if (process != null) {
                    ItemDao.update(item);
                }

                refreshWeight();

                return item;
            }

            if (item.getCount() < count) {
                return null;
            }

            boolean removed = removeItem(item);
            if (!removed) {
                return null;
            }

            item.destroyMe(process, actor, reference);
            ItemDao.remove(item.getObjectId());
            refreshWeight();
        }
        return item;
    }

    /**
     * Destroy item from inventory by using its <B>objectID</B> and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param objectId : int Item Instance identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param actor : Player Player requesting the item destroy
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public ItemInstance destroyItem(String process, int objectId, int count, Player actor, WorldObject reference) {
        ItemInstance item = getItemByObjectId(objectId);
        if (item == null) {
            return null;
        }

        return destroyItem(process, item, count, actor, reference);
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param actor : Player Player requesting the item destroy
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public ItemInstance destroyItemByItemId(String process, int itemId, int count, Player actor, WorldObject reference) {
        ItemInstance item = getItemByItemId(itemId);
        if (item == null) {
            return null;
        }

        return destroyItem(process, item, count, actor, reference);
    }

    /**
     * Destroy all items from inventory and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param actor : Player Player requesting the item destroy
     * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     */
    public void destroyAllItems(String process, Player actor, WorldObject reference) {
        for (ItemInstance item : _items) {
            destroyItem(process, item, actor, reference);
        }
    }

    /**
     * @return the amount of adena (itemId 57)
     */
    public int getAdena() {
        for (ItemInstance item : _items) {
            if (item.getItemId() == 57) {
                return item.getCount();
            }
        }
        return 0;
    }

    /**
     * Adds item to inventory for further adjustments.
     *
     * @param item : ItemInstance to be added from inventory
     */
    protected void addItem(ItemInstance item) {
        _items.add(item);
    }

    /**
     * Removes item from inventory for further adjustments.
     *
     * @param item : ItemInstance to be removed from inventory
     * @return
     */
    protected boolean removeItem(ItemInstance item) {
        return _items.remove(item);
    }

    /**
     * Refresh the weight of equipment loaded
     */
    protected void refreshWeight() {
    }

    /**
     * Delete item object from world
     */
    public void deleteMe() {
        if (getOwner() != null) {
            for (ItemInstance item : _items) {
                ItemDao.update(item);
                World.getInstance().removeObject(item);
            }
        }
        _items.clear();
    }

    /**
     * Get back items in container from database
     */
    public void restore() {
        InventoryDao.restore(this);
        refreshWeight();
    }

    public boolean validateCapacity(int slotCount) {
        return true;
    }

    public boolean validateWeight(int weight) {
        return true;
    }
}