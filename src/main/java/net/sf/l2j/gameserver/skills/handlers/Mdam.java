package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.Map;

public class Mdam extends Default {

    public Mdam(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        boolean sps = caster.isChargedShot(ShotType.SPIRITSHOT);
        boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isDead()) {
                continue;
            }

            final boolean isCrit = Formulas.calcMCrit(caster, target, this);
            final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, false);
            final byte reflect = Formulas.calcSkillReflect(target, this);

            int damage = (int) Formulas.calcMagicDam(caster, target, this, sDef, sps, bsps, isCrit);
            if (damage > 0) {
                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, damage);

                // vengeance reflected damage
                if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
                    caster.reduceCurrentHp(damage, target, this);
                } else {
                    caster.sendDamageMessage(target, damage, isCrit, false, false);
                    target.reduceCurrentHp(damage, caster, this);
                }

                if (hasEffects() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) == null) {
                    if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0) // reflect skill effects
                    {
                        caster.stopSkillEffects(getId());
                        applyEffects(target, caster);
                    } else {
                        // activate attacked effects, if any
                        target.stopSkillEffects(getId());
                        if (Formulas.calcSkillSuccess(caster, target, this, sDef, bsps)) {
                            applyEffects(caster, target, sDef, bsps);
                        } else {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
                        }
                    }
                }

            }

            notifyAboutSkillHit(caster, target, Map.of("damage", damage));
        }

        if (hasSelfEffects()) {
            final AbstractEffect effect = caster.getFirstEffect(getId());
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }

            applySelfEffects(caster);
        }

        if (isSuicideAttack()) {
            caster.doDie(caster);
        }

        caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public boolean isDamage() {
        return true;
    }
}