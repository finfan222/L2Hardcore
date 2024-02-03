package net.sf.l2j.gameserver.skills.handlers;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.extractable.ExtractableProductItem;
import net.sf.l2j.gameserver.skills.extractable.ExtractableSkill;

@Slf4j
public class Extractable extends L2Skill {

    public Extractable(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player)) {
            return;
        }

        final ExtractableSkill exItem = getExtractableSkill();
        if (exItem == null || exItem.getProductItems().isEmpty()) {
            log.warn("Missing informations for extractable skill id: {}.", getId());
            return;
        }

        final Player player = caster.getActingPlayer();

        int chance = Rnd.get(100000);
        boolean created = false;
        for (ExtractableProductItem expi : exItem.getProductItems()) {
            chance -= (int) (expi.getChance() * 1000);
            if (chance >= 0) {
                continue;
            }

            // The inventory is full, terminate.
            if (!player.getInventory().validateCapacityByItemIds(expi.getItems())) {
                player.sendPacket(SystemMessageId.SLOTS_FULL);
                return;
            }

            // Inventory has space, create all items.
            for (IntIntHolder item : expi.getItems()) {
                player.addItem("Extract", item.getId(), item.getValue(), player, true);
                created = true;
            }

            break;
        }

        if (!created) {
            player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
        }
    }
}