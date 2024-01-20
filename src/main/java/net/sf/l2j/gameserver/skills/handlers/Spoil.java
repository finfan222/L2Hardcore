package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.Map;

public class Spoil extends Default {

    public Spoil(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player)) {
            return;
        }

        if (targets == null) {
            return;
        }

        for (WorldObject tgt : targets) {
            if (!(tgt instanceof Monster target)) {
                continue;
            }

            if (target.isDead()) {
                continue;
            }

            if (target.getSpoilState().isSpoiled()) {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ALREADY_SPOILED));
                continue;
            }

            if (Formulas.calcMagicSuccess(caster, (Creature) tgt, this)) {
                target.getSpoilState().setSpoilerId(caster.getObjectId());
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SPOIL_SUCCESS));
            } else {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
            }

            notifyAboutSkillHit(caster, target, Map.of("damage", caster.getStatus().getLevel() * 120));
        }
    }
}