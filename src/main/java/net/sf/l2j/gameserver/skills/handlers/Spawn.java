package net.sf.l2j.gameserver.skills.handlers;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.skills.L2Skill;

@Slf4j
public class Spawn extends L2Skill {
    private final int _npcId;
    private final int _despawnDelay;

    public Spawn(StatSet set) {
        super(set);

        _npcId = set.getInteger("npcId", 0);
        _despawnDelay = set.getInteger("despawnDelay", 0);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        try {
            // Create spawn.
            final net.sf.l2j.gameserver.model.spawn.Spawn spawn = new net.sf.l2j.gameserver.model.spawn.Spawn(_npcId);
            spawn.setRespawnState(false);
            spawn.setLoc(caster.getPosition());

            // Spawn NPC.
            final Npc npc = spawn.doSpawn(false);
            if (_despawnDelay > 0) {
                npc.scheduleDespawn(_despawnDelay);
            }
        } catch (Exception e) {
            log.error("Failed to initialize a spawn.", e);
        }
    }
}