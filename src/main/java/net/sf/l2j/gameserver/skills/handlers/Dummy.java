package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Dummy extends L2Skill {

    public Dummy(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return getSkillType() == SkillType.DELUXE_KEY_UNLOCK || getSkillType() == SkillType.BEAST_FEED;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        //todo: что за хуйня?
        if (caster instanceof Player player) {
            if (getSkillType() == SkillType.BEAST_FEED) {
                final WorldObject target = targets[0];
                if (target == null) {
                    return;
                }
            }
        }
    }
}