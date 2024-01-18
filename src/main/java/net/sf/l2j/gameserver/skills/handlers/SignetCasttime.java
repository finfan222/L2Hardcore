package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

public final class SignetCasttime extends L2Skill {
    public final int effectNpcId;
    public final int effectId;

    public SignetCasttime(StatSet set) {
        super(set);
        effectNpcId = set.getInteger("effectNpcId", -1);
        effectId = set.getInteger("effectId", -1);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        applySelfEffects(caster);
    }

    @Override
    public boolean isDamage() {
        return true;
    }
}