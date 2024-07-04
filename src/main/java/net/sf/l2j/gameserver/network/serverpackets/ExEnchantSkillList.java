package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.holder.skillnode.EnchantSkillNode;

import java.util.List;

public class ExEnchantSkillList extends L2GameServerPacket {
    private final List<EnchantSkillNode> _skills;

    public ExEnchantSkillList(List<EnchantSkillNode> skills) {
        _skills = skills;
    }

    @Override
    protected void writeImpl() {
        writeC(0xfe);
        writeH(0x17);

        writeD(_skills.size());
        for (EnchantSkillNode esn : _skills) {
            writeD(esn.getId());
            writeD(esn.getValue());
            writeD(esn.getSp());
            writeQ(esn.getExp());
        }
    }
}