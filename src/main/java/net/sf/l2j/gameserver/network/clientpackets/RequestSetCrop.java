package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.manor.CropProcure;
import net.sf.l2j.gameserver.model.manor.Seed;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

import java.util.ArrayList;
import java.util.List;

public class RequestSetCrop extends L2GameClientPacket {
    private static final int BATCH_LENGTH = 13;

    private int _manorId;
    private List<CropProcure> _items;

    @Override
    protected void readImpl() {
        _manorId = readD();
        final int count = readD();
        if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || (count * BATCH_LENGTH) != _buf.remaining()) {
            return;
        }

        _items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final int itemId = readD();
            final int sales = readD();
            final int price = readD();
            final int type = readC();

            if (itemId < 1 || sales < 0 || price < 0) {
                _items.clear();
                return;
            }

            if (sales > 0) {
                _items.add(new CropProcure(itemId, sales, type, sales, price));
            }
        }
    }

    @Override
    protected void runImpl() {
        if (_items.isEmpty()) {
            return;
        }

        final CastleManorManager manor = CastleManorManager.getInstance();
        if (!manor.isModifiablePeriod()) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Check player privileges
        final Player player = getClient().getPlayer();
        if (player == null || player.getClan() == null || player.getClan().getCastleId() != _manorId || !player.hasClanPrivileges(Clan.CP_CS_MANOR_ADMIN) || !player.getAI().canDoInteract(player.getCurrentFolk())) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Filter crops with start amount lower than 0 and incorrect price
        final List<CropProcure> list = new ArrayList<>(_items.size());
        for (CropProcure cp : _items) {
            final Seed s = manor.getSeedByCrop(cp.getId(), _manorId);
            if (s != null && cp.getStartAmount() <= s.getCropLimit() && cp.getPrice() >= s.getCropMinPrice() && cp.getPrice() <= s.getCropMaxPrice()) {
                list.add(cp);
            }
        }

        // Save crop list
        manor.setNextCropProcure(list, _manorId);
    }
}