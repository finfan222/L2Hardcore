package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class CreateItem extends L2Skill {
    private final int[] _createItemId;
    private final int _createItemCount;
    private final int _randomCount;

    public CreateItem(StatSet set) {
        super(set);
        _createItemId = set.getIntegerArray("create_item_id");
        _createItemCount = set.getInteger("create_item_count", 0);
        _randomCount = set.getInteger("random_count", 1);
    }

    /**
     * @see net.sf.l2j.gameserver.skills.L2Skill#useSkill(net.sf.l2j.gameserver.model.actor.Creature,
     * net.sf.l2j.gameserver.model.WorldObject[])
     */
    @Override
    public void useSkill(Creature activeChar, WorldObject[] targets) {
        Player player = activeChar.getActingPlayer();
        if (activeChar.isAlikeDead()) {
            return;
        }

        if (activeChar instanceof Playable) {
            if (_createItemId == null || _createItemCount == 0) {
                SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
                sm.addSkillName(this);
                activeChar.sendPacket(sm);
                return;
            }

            int count = _createItemCount + Rnd.get(_randomCount);
            int rndid = Rnd.get(_createItemId.length);

            if (activeChar instanceof Player) {
                player.addItem("Skill", _createItemId[rndid], count, activeChar, true);
            } else if (activeChar instanceof Pet) {
                activeChar.getInventory().addItem("Skill", _createItemId[rndid], count, player, activeChar);
                player.sendPacket(new PetItemList((Pet) activeChar));
            }
        }
    }
}