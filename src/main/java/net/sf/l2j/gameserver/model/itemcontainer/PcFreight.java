package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class PcFreight extends ItemContainer {
    private final Player _owner;

    private int _activeLocationId;
    private int _tempOwnerId = 0;

    public PcFreight(Player owner) {
        _owner = owner;
    }

    @Override
    public String getName() {
        return "Freight";
    }

    @Override
    public Player getOwner() {
        return _owner;
    }

    @Override
    public ItemLocation getBaseLocation() {
        return ItemLocation.FREIGHT;
    }

    public void setActiveLocation(int locationId) {
        _activeLocationId = locationId;
    }

    @Override
    public int getSize() {
        int size = 0;
        for (ItemInstance item : _items) {
            if (item.getSlot() == 0 || _activeLocationId == 0 || item.getSlot() == _activeLocationId) {
                size++;
            }
        }
        return size;
    }

    @Override
    public Set<ItemInstance> getItems() {
        if (_items.isEmpty()) {
            return Collections.emptySet();
        }

        return _items.stream().filter(i -> i.getSlot() == 0 || i.getSlot() == _activeLocationId).collect(Collectors.toSet());
    }

    @Override
    public ItemInstance getItemByItemId(int itemId) {
        for (ItemInstance item : _items) {
            if (item.getItemId() == itemId && (item.getSlot() == 0 || _activeLocationId == 0 || item.getSlot() == _activeLocationId)) {
                return item;
            }
        }
        return null;
    }

    @Override
    protected void addItem(ItemInstance item) {
        super.addItem(item);

        if (_activeLocationId > 0) {
            item.setLocation(item.getLocation());
            item.setSlot(_activeLocationId);
        }
    }

    @Override
    public void restore() {
        int locationId = _activeLocationId;
        _activeLocationId = 0;

        super.restore();

        _activeLocationId = locationId;
    }

    @Override
    public boolean validateCapacity(int slotCount) {
        if (slotCount == 0) {
            return true;
        }

        return getSize() + slotCount <= ((_owner == null) ? Config.FREIGHT_SLOTS : _owner.getStatus().getFreightLimit());
    }

    @Override
    public int getOwnerId() {
        return (_owner == null) ? _tempOwnerId : super.getOwnerId();
    }

    /**
     * This provides support to load a new PcFreight without owner so that transactions can be done
     *
     * @param val The id of the owner.
     */
    public void doQuickRestore(int val) {
        _tempOwnerId = val;

        restore();
    }
}