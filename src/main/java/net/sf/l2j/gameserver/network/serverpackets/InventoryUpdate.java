package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.model.item.instance.ItemInfo;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.ArrayList;
import java.util.List;

public class InventoryUpdate extends L2GameServerPacket {
    private List<ItemInfo> _items;

    public InventoryUpdate() {
        _items = new ArrayList<>();
    }

    public InventoryUpdate(List<ItemInfo> items) {
        _items = items;
    }

    public void addItem(ItemInstance item) {
        if (item != null) {
            _items.add(new ItemInfo(item));
        }
    }

    public void addNewItem(ItemInstance item) {
        if (item != null) {
            _items.add(new ItemInfo(item, ItemState.ADDED));
        }
    }

    public void addModifiedItem(ItemInstance item) {
        if (item != null) {
            _items.add(new ItemInfo(item, ItemState.MODIFIED));
        }
    }

    public void addRemovedItem(ItemInstance item) {
        if (item != null) {
            _items.add(new ItemInfo(item, ItemState.REMOVED));
        }
    }

    public void addItems(List<ItemInstance> items) {
        if (items != null) {
            for (ItemInstance item : items) {
                if (item != null) {
                    _items.add(new ItemInfo(item));
                }
            }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x27);
        writeH(_items.size());

        for (ItemInfo temp : _items) {
            Item item = temp.getItem();

            writeH(temp.getChange().ordinal());
            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(item.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeH(temp.getEquipped());
            writeD(item.getBodyPart());
            writeH(temp.getEnchant());
            writeH(temp.getCustomType2());
            writeD(temp.getAugmentationBoni());
            writeD(temp.getMana());
        }
    }
}