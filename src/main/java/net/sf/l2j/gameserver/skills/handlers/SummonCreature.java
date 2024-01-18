package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.SummonItemData;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SummonCreature extends L2Skill {

    public SummonCreature(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        final ItemInstance item = player.getInventory().getItemByObjectId(player.getAI().getCurrentIntention().getItemObjectId());

        // Skill cast may have been interrupted of cancelled
        if (item == null) {
            return;
        }

        // Check for summon item validity.
        if (item.getOwnerId() != player.getObjectId() || item.getLocation() != ItemLocation.INVENTORY) {
            return;
        }

        // Owner has a pet listed in world.
        if (World.getInstance().getPet(player.getObjectId()) != null) {
            return;
        }

        // Check summon item validity.
        final IntIntHolder summonItem = SummonItemData.getInstance().getSummonItem(item.getItemId());
        if (summonItem == null) {
            return;
        }

        // Check NpcTemplate validity.
        final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(summonItem.getId());
        if (npcTemplate == null) {
            return;
        }

        // Add the pet instance to world.
        final Pet pet = Pet.restore(item, npcTemplate, player);
        if (pet == null) {
            return;
        }

        World.getInstance().addPet(player.getObjectId(), pet);

        player.setSummon(pet);

        pet.forceRunStance();
        pet.setTitle(player.getName());
        pet.startFeed();

        final SpawnLocation spawnLoc = caster.getPosition().clone();
        spawnLoc.addStrictOffset(40);
        spawnLoc.setHeadingTo(caster.getPosition());
        spawnLoc.set(GeoEngine.getInstance().getValidLocation(caster, spawnLoc));

        pet.spawnMe(spawnLoc);
        pet.getAI().setFollowStatus(true);
    }
}