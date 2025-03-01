package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.model.manor.Seed;

import java.util.List;

public class ExShowManorDefaultInfo extends L2GameServerPacket {
    private final List<Seed> _crops;
    private final boolean _hideButtons;

    public ExShowManorDefaultInfo(boolean hideButtons) {
        _crops = CastleManorManager.getInstance().getCrops();
        _hideButtons = hideButtons;
    }

    @Override
    protected void writeImpl() {
        writeC(0xFE);
        writeH(0x1E);
        writeC(_hideButtons ? 0x01 : 0x00);
        writeD(_crops.size());
        for (Seed crop : _crops) {
            writeD(crop.getCropId()); // crop Id
            writeD(crop.getLevel()); // level
            writeD(crop.getSeedReferencePrice()); // seed price
            writeD(crop.getCropReferencePrice()); // crop price
            writeC(1); // Reward 1 type
            writeD(crop.getReward(1)); // Reward 1 itemId
            writeC(1); // Reward 2 type
            writeD(crop.getReward(2)); // Reward 2 itemId
        }
    }
}