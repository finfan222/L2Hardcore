package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.model.manor.Seed;
import net.sf.l2j.gameserver.model.manor.SeedProduction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExShowSeedSetting extends L2GameServerPacket {
    private final int _manorId;
    private final Set<Seed> _seeds;
    private final Map<Integer, SeedProduction> _current = new HashMap<>();
    private final Map<Integer, SeedProduction> _next = new HashMap<>();

    public ExShowSeedSetting(int manorId) {
        final CastleManorManager manor = CastleManorManager.getInstance();

        _manorId = manorId;
        _seeds = manor.getSeedsForCastle(_manorId);

        for (Seed s : _seeds) {
            // Current period
            SeedProduction sp = manor.getSeedProduct(manorId, s.getSeedId(), false);
            if (sp != null) {
                _current.put(s.getSeedId(), sp);
            }

            // Next period
            sp = manor.getSeedProduct(manorId, s.getSeedId(), true);
            if (sp != null) {
                _next.put(s.getSeedId(), sp);
            }
        }
    }

    @Override
    public void writeImpl() {
        writeC(0xFE);
        writeH(0x1F);

        writeD(_manorId);
        writeD(_seeds.size());

        SeedProduction sp;
        for (Seed s : _seeds) {
            writeD(s.getSeedId()); // seed id
            writeD(s.getLevel()); // level
            writeC(1);
            writeD(s.getReward(1)); // reward 1 id
            writeC(1);
            writeD(s.getReward(2)); // reward 2 id

            writeD(s.getSeedLimit()); // next sale limit
            writeD(s.getSeedReferencePrice()); // price for castle to produce 1
            writeD(s.getSeedMinPrice()); // min seed price
            writeD(s.getSeedMaxPrice()); // max seed price

            // Current period
            if (_current.containsKey(s.getSeedId())) {
                sp = _current.get(s.getSeedId());
                writeD(sp.getStartAmount()); // sales
                writeD(sp.getPrice()); // price
            } else {
                writeD(0);
                writeD(0);
            }
            // Next period
            if (_next.containsKey(s.getSeedId())) {
                sp = _next.get(s.getSeedId());
                writeD(sp.getStartAmount()); // sales
                writeD(sp.getPrice()); // price
            } else {
                writeD(0);
                writeD(0);
            }
        }
    }
}