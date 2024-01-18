package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.List;

public class Harvest extends L2Skill {

    public Harvest(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    private static boolean calcSuccess(Creature activeChar, Creature target) {
        int basicSuccess = 100;
        final int levelPlayer = activeChar.getStatus().getLevel();
        final int levelTarget = target.getStatus().getLevel();

        int diff = (levelPlayer - levelTarget);
        if (diff < 0) {
            diff = -diff;
        }

        // apply penalty, target <=> player levels, 5% penalty for each level
        if (diff > 5) {
            basicSuccess -= (diff - 5) * 5;
        }

        // success rate cant be less than 1%
        if (basicSuccess < 1) {
            basicSuccess = 1;
        }

        return Rnd.get(99) < basicSuccess;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        final WorldObject object = targets[0];
        if (!(object instanceof Monster target)) {
            return;
        }

        if (!target.getSeedState().isActualSeeder(player)) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
            return;
        }

        if (!target.getSeedState().isSeeded()) {
            player.sendPacket(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
            return;
        }

        if (!calcSuccess(player, target)) {
            player.sendPacket(SystemMessageId.THE_HARVEST_HAS_FAILED);
            return;
        }

        final List<IntIntHolder> items = target.getSeedState();
        if (items.isEmpty()) {
            return;
        }

        boolean send = false;
        int total = 0;
        int cropId = 0;

        InventoryUpdate iu = new InventoryUpdate();
        for (IntIntHolder ritem : items) {
            cropId = ritem.getId(); // always got 1 type of crop as reward

            if (player.isInParty()) {
                player.getParty().distributeItem(player, ritem, true, target);
            } else {
                ItemInstance item = player.getInventory().addItem("Manor", ritem.getId(), ritem.getValue(), player, target);
                iu.addItem(item);

                send = true;
                total += ritem.getValue();
            }
        }

        if (send) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(cropId).addNumber(total));

            if (player.isInParty()) {
                player.getParty().broadcastToPartyMembers(player, SystemMessage.getSystemMessage(SystemMessageId.S1_HARVESTED_S3_S2S).addCharName(player).addItemName(cropId).addNumber(total));
            }

            player.sendPacket(iu);
        }

        // Reset variables.
        target.getSeedState().clear();
    }
}