package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ConfirmDlg extends L2GameServerPacket {

    private static final int TYPE_ZONE_NAME = 7;
    private static final int TYPE_SKILL_NAME = 4;
    private static final int TYPE_ITEM_NAME = 3;
    private static final int TYPE_NPC_NAME = 2;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_TEXT = 0;

    @Getter
    private final int messageId;
    private final List<CnfDlgData> info = new ArrayList<>();

    private int time;
    @Getter private int requesterId;

    public ConfirmDlg(int messageId) {
        this.messageId = messageId;
    }

    public ConfirmDlg(SystemMessageId messageId) {
        this.messageId = messageId.getId();
    }

    public ConfirmDlg addString(String text) {
        info.add(new CnfDlgData(TYPE_TEXT, text));
        return this;
    }

    public ConfirmDlg addNumber(int number) {
        info.add(new CnfDlgData(TYPE_NUMBER, number));
        return this;
    }

    public ConfirmDlg addCharName(Creature cha) {
        return addString(cha.getName());
    }

    public ConfirmDlg addItemName(ItemInstance item) {
        return addItemName(item.getItem().getItemId());
    }

    public ConfirmDlg addItemName(Item item) {
        return addItemName(item.getItemId());
    }

    public ConfirmDlg addItemName(int id) {
        info.add(new CnfDlgData(TYPE_ITEM_NAME, id));
        return this;
    }

    public ConfirmDlg addZoneName(Location loc) {
        info.add(new CnfDlgData(TYPE_ZONE_NAME, loc));
        return this;
    }

    public ConfirmDlg addSkillName(AbstractEffect effect) {
        return addSkillName(effect.getSkill());
    }

    public ConfirmDlg addSkillName(L2Skill skill) {
        return addSkillName(skill.getId(), skill.getLevel());
    }

    public ConfirmDlg addSkillName(int id) {
        return addSkillName(id, 1);
    }

    public ConfirmDlg addSkillName(int id, int lvl) {
        info.add(new CnfDlgData(TYPE_SKILL_NAME, new IntIntHolder(id, lvl)));
        return this;
    }

    public ConfirmDlg addTime(int time) {
        this.time = time;
        return this;
    }

    public ConfirmDlg addRequesterId(int id) {
        requesterId = id;
        return this;
    }

    @Override
    protected final void writeImpl() {
        writeC(0xed);
        writeD(messageId);

        if (info.isEmpty()) {
            writeD(0x00);
            writeD(time);
            writeD(requesterId);
        } else {
            writeD(info.size());

            for (CnfDlgData data : info) {
                writeD(data.getType());

                switch (data.getType()) {
                    case TYPE_TEXT:
                        writeS((String) data.getObject());
                        break;

                    case TYPE_NUMBER:
                    case TYPE_NPC_NAME:
                    case TYPE_ITEM_NAME:
                        writeD((Integer) data.getObject());
                        break;

                    case TYPE_SKILL_NAME:
                        final IntIntHolder info = (IntIntHolder) data.getObject();
                        writeD(info.getId());
                        writeD(info.getValue());
                        break;

                    case TYPE_ZONE_NAME:
                        writeLoc((Location) data.getObject());
                        break;
                }
            }
            if (time != 0) {
                writeD(time);
            }
            if (requesterId != 0) {
                writeD(requesterId);
            }
        }
    }

    private static final class CnfDlgData {
        private final int _type;
        private final Object _value;

        protected CnfDlgData(int type, Object val) {
            _type = type;
            _value = val;
        }

        public int getType() {
            return _type;
        }

        public Object getObject() {
            return _value;
        }
    }
}