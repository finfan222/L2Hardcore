package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.trade.TradeItem;

import java.util.List;

public class PrivateStoreManageListBuy extends L2GameServerPacket {
    private final int _objId;
    private final int _playerAdena;
    private final ItemInstance[] _itemList;
    private final List<TradeItem> _buyList;

    public PrivateStoreManageListBuy(Player player) {
        _objId = player.getObjectId();
        _playerAdena = player.getAdena();
        _itemList = player.getInventory().getUniqueItems(false, true, true);
        _buyList = player.getBuyList();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xb7);
        writeD(_objId);
        writeD(_playerAdena);

        writeD(_itemList.length); // inventory items for potential buy
        for (ItemInstance item : _itemList) {
            writeD(item.getItemId());
            writeH(item.getEnchantLevel());
            writeD(item.getCount());
            writeD(item.getReferencePrice());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeH(item.getItem().getType2());
        }

        writeD(_buyList.size()); // count for all items already added for buy
        for (TradeItem item : _buyList) {
            writeD(item.getItem().getItemId());
            writeH(item.getEnchant());
            writeD(item.getQuantity());
            writeD(item.getItem().getReferencePrice());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeH(item.getItem().getType2());
            writeD(item.getPrice());// your price
            writeD(item.getItem().getReferencePrice());// fixed store price
        }
    }
}