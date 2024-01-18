package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Manadam extends L2Skill {

    public Manadam(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        final boolean sps = caster.isChargedShot(ShotType.SPIRITSHOT);
        final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature)) {
                continue;
            }

            Creature target = ((Creature) obj);
            if (Formulas.calcSkillReflect(target, this) == Formulas.SKILL_REFLECT_SUCCEED) {
                target = caster;
            }

            boolean acted = Formulas.calcMagicAffected(caster, target, this);
            if (target.isInvul() || !acted) {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MISSED_TARGET));
            } else {
                if (hasEffects()) {
                    target.stopSkillEffects(getId());

                    final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, false);
                    if (Formulas.calcSkillSuccess(caster, target, this, sDef, bsps)) {
                        applyEffects(caster, target, sDef, bsps);
                    } else {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                    }
                }

                double damage = Formulas.calcManaDam(caster, target, this, sps, bsps);

                double mp = Math.min(damage, target.getStatus().getMp());
                target.getStatus().reduceMp(mp);
                if (damage > 0) {
                    target.stopEffects(EffectType.SLEEP);
                    target.stopEffects(EffectType.IMMOBILE_UNTIL_ATTACKED);
                }

                if (target instanceof Player) {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_MP_HAS_BEEN_DRAINED_BY_S1).addCharName(caster).addNumber((int) mp));
                }

                if (caster instanceof Player) {
                    caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1).addNumber((int) mp));
                }
            }
        }

        if (hasSelfEffects()) {
            final AbstractEffect effect = caster.getFirstEffect(getId());
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }

            applySelfEffects(caster);
        }

        caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }
}