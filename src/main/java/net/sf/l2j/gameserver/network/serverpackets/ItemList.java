package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.Set;

public class ItemList extends L2GameServerPacket {
    private final Set<ItemInstance> _items;
    private final boolean _showWindow;

    public ItemList(Player cha, boolean showWindow) {
        _items = cha.getInventory().getItems();
        _showWindow = showWindow;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x1b);
        writeH(_showWindow ? 0x01 : 0x00);
        writeH(_items.size());

        for (ItemInstance temp : _items) {
            Item item = temp.getItem();

            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(temp.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeH(temp.isEquipped() ? 0x01 : 0x00);
            writeD(item.getBodyPart());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
            writeD((temp.isAugmented()) ? temp.getAugmentation().getId() : 0x00);
            writeD(temp.getDurabilityPercent());
        }
    }
}