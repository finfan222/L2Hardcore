package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

public class ChargeDmg extends Default {
    public ChargeDmg(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        double modifier = 0;

        if (caster instanceof Player) {
            modifier = 0.8 + 0.2 * (((Player) caster).getCharges() + getNumCharges());
        }

        final boolean ss = caster.isChargedShot(ShotType.SOULSHOT);
        Context context = Context.builder().build();

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isAlikeDead()) {
                continue;
            }

            context.isMissed = isEvaded(caster, target);
            if (context.isMissed) {
                continue;
            }

            context.isCritical = getBaseCritRate() > 0 && Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
            context.block = Formulas.calcShldUse(caster, target, this, context.isCritical);
            context.value = Formulas.calcPhysicalSkillDamage(caster, target, this, context.block, context.isCritical, ss);

            if (context.value > 0) {
                context.value *= modifier;

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

                caster.sendDamageMessage(target, (int) context.value, false, context.isCritical, false);
            } else {
                caster.sendDamageMessage(target, 0, false, false, true);
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

        caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());
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