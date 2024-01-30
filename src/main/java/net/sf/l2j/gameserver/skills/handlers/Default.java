package net.sf.l2j.gameserver.skills.handlers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.events.OnSkillHit;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.io.Serial;
import java.io.Serializable;

public class Default extends L2Skill {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        double value;
        boolean isCritical;
        boolean isReflected;
        boolean isMissed;
        @Builder.Default
        boolean isSuccess = true;
        boolean isAvenged;
        @Builder.Default
        ShieldDefense block = ShieldDefense.FAILED;

    }

    public Default(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        caster.sendPacket(ActionFailed.STATIC_PACKET);
        caster.sendMessage("Skill " + getId() + " [" + getSkillType() + "] isn't implemented.");
    }

    protected void notifyAboutSkillHit(Creature caster, Creature target, Context context) {
        OnSkillHit onSkillHit = OnSkillHit.builder()
            .caster(caster)
            .target(target)
            .skill(this)
            .context(context)
            .build();
        caster.getEventListener().notify(onSkillHit);
        target.getEventListener().notify(onSkillHit);
    }

    protected boolean isAvenged(Creature caster, Creature target) {
        boolean isAvenged = Formulas.calcSkillVengeance(target, this);

        if (isAvenged) {
            if (target instanceof Player) {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(caster));
            }

            if (caster instanceof Player) {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
            }
        }

        return isAvenged;
    }

    protected boolean isEvaded(Creature caster, Creature target) {
        boolean isEvaded = Formulas.calcPhysicalSkillEvasion(target, this);
        if (isEvaded) {
            if (caster instanceof Player) {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
            }

            if (target instanceof Player) {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(caster));
            }
        }

        return isEvaded;
    }

    protected boolean isReflected(Creature caster, Creature target, ShieldDefense block) {
        boolean isAffected = Formulas.calcSkillSuccess(caster, target, this, block, true);
        if (isAffected) {
            boolean isReflected = Formulas.calcEffectReflect(target, this);
            if (isReflected) {
                if (caster instanceof Player) {
                    caster.sendMessage("[FIX-SYSMSG] $s1 сумел отразить эффект $s2 и вернуть его вам!");
                }

                if (target instanceof Player) {
                    target.sendMessage("[FIX-SYSMSG] Вы отразили $s1 эффект обратно на заклинателя!");
                }
            }

            return isReflected;
        } else {
            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
        }

        return false;
    }
}