package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.events.OnSkillHit;
import net.sf.l2j.gameserver.events.OnSkillHitBy;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.io.Serializable;
import java.util.Map;

public class Default extends L2Skill {
    public Default(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        caster.sendPacket(ActionFailed.STATIC_PACKET);
        caster.sendMessage("Skill " + getId() + " [" + getSkillType() + "] isn't implemented.");
    }

    protected void notifyAboutSkillHit(Creature caster, Creature target, Map<String, Serializable> context) {
        OnSkillHit onSkillHit = OnSkillHit.builder()
            .caster(caster)
            .target(target)
            .skill(this)
            .build();
        onSkillHit.getContext().putAll(context);
        caster.getEventListener().notify(onSkillHit);

        OnSkillHitBy onSkillHitBy = OnSkillHitBy.builder()
            .caster(caster)
            .target(target)
            .skill(this)
            .build();
        onSkillHitBy.getContext().putAll(context);
        target.getEventListener().notify(onSkillHitBy);
    }
}