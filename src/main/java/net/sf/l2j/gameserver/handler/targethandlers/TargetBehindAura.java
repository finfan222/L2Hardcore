package net.sf.l2j.gameserver.handler.targethandlers;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.ITargetHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.ArrayList;
import java.util.List;

public class TargetBehindAura implements ITargetHandler {
    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.BEHIND_AURA;
    }

    @Override
    public Creature[] getTargetList(Creature caster, Creature target, L2Skill skill) {
        final List<Creature> list = new ArrayList<>();
        for (Creature creature : caster.getKnownTypeInRadius(Creature.class, skill.getSkillRadius())) {
            if (creature.isDead() || !creature.isBehind(caster) || !GeoEngine.getInstance().canSeeTarget(caster, creature)) {
                continue;
            }

            if (caster instanceof Playable && (creature instanceof Attackable || creature instanceof Playable)) {
                if (creature.isAttackableWithoutForceBy((Playable) caster)) {
                    list.add(creature);
                }
            } else if (caster instanceof Attackable && creature instanceof Playable) {
                if (creature.isAttackableBy(caster)) {
                    list.add(creature);
                }
            }
        }

        if (list.isEmpty()) {
            return EMPTY_TARGET_ARRAY;
        }

        return list.toArray(new Creature[list.size()]);
    }

    @Override
    public Creature getFinalTarget(Creature caster, Creature target, L2Skill skill) {
        return caster;
    }

    @Override
    public boolean meetCastConditions(Playable caster, Creature target, L2Skill skill, boolean isCtrlPressed) {
        if (caster.isInsideZone(ZoneId.PEACE)) {
            caster.sendPacket(SystemMessageId.CANT_ATK_PEACEZONE);
            return false;
        }
        return true;
    }
}