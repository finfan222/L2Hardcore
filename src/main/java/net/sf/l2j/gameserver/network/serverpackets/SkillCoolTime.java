package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.Timestamp;

import java.util.List;
import java.util.stream.Collectors;

public class SkillCoolTime extends L2GameServerPacket {
    public List<Timestamp> _reuseTimeStamps;

    public SkillCoolTime(Player cha) {
        _reuseTimeStamps = cha.getReuseTimeStamps().stream().filter(r -> r.hasNotPassed()).collect(Collectors.toList());
    }

    @Override
    protected void writeImpl() {
        writeC(0xc1);
        writeD(_reuseTimeStamps.size()); // list size
        for (Timestamp ts : _reuseTimeStamps) {
            writeD(ts.getId());
            writeD(ts.getValue());
            writeD((int) ts.getReuse() / 1000);
            writeD((int) ts.getRemaining() / 1000);
        }
    }
}