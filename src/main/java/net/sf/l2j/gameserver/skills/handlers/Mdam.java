package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

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

        Context context = Context.builder().build();

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isDead()) {
                continue;
            }

            context.isCritical = Formulas.calcMCrit(caster, target, this);
            context.block = Formulas.calcShldUse(caster, target, this, false);
            context.value = (int) Formulas.calcMagicDam(caster, target, this, context.block, sps, bsps, context.isCritical);

            if (context.value > 0) {
                caster.sendDamageMessage(target, (int) context.value, context.isCritical, false, false);

                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, context.value);

                // vengeance reflected damage
                context.isAvenged = isAvenged(caster, target);
                if (context.isAvenged) {
                    caster.reduceCurrentHp(context.value, target, this);
                } else {
                    target.reduceCurrentHp(context.value, caster, this);
                }

                if (hasEffects() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) == null) {
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        caster.stopSkillEffects(this.getId());
                        applyEffects(target, caster);
                    } else {
                        target.stopSkillEffects(this.getId());
                        applyEffects(caster, target, context.block, false);
                    }
                }
            } else {
                caster.sendDamageMessage(target, 0, false, false, false);
            }

            notifyAboutSkillHit(caster, target, context);
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