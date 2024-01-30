package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

public class Blow extends Default {

    public Blow(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        final boolean ss = caster.isChargedShot(ShotType.SOULSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isAlikeDead()) {
                continue;
            }

            Context context = Context.builder().build();

            if (Formulas.calcBlowRate(caster, target, this)) {
                // Calculate skill evasion.
                context.isMissed = isEvaded(caster, target);
                if (context.isMissed) {
                    continue;
                }

                context.isCritical = getBaseCritRate() > 0 && Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
                context.block = Formulas.calcShldUse(caster, target, this, context.isCritical);

                if (hasEffects()) {
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        caster.stopSkillEffects(this.getId());
                        applyEffects(target, caster);
                    } else {
                        target.stopSkillEffects(this.getId());
                        applyEffects(caster, target, context.block, false);
                    }
                }

                context.value = (int) Formulas.calcBlowDamage(caster, target, this, context.block, ss);
                if (context.isCritical) {
                    context.value *= 2;
                }

                // vengeance reflected damage
                context.isAvenged = isAvenged(caster, target);
                if (context.isAvenged) {
                    caster.reduceCurrentHp(context.value, target, this);
                } else {
                    target.reduceCurrentHp(context.value, caster, this);
                }

                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, context.value);

                // Send damage message.
                caster.sendDamageMessage(target, (int) context.value, false, true, false);
                caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());

            }

            notifyAboutSkillHit(caster, target, context);

            // Possibility of a lethal strike
            Formulas.calcLethalHit(caster, target, this);

            if (hasSelfEffects()) {
                final AbstractEffect effect = caster.getFirstEffect(getId());
                if (effect != null && effect.isSelfEffect()) {
                    effect.exit();
                }

                applySelfEffects(caster);
            }
        }
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