package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.effects.EffectFear;

public class Pdam extends Default {

    public Pdam(StatSet set) {
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

            if (target.isDead()) {
                continue;
            }

            if (target instanceof Playable && ArraysUtil.contains(EffectFear.DOESNT_AFFECT_PLAYABLE, getId())) {
                continue;
            }

            Context context = Context.builder().build();
            context.isMissed = isEvaded(caster, target);
            if (context.isMissed) {
                continue;
            }

            context.isCritical = getBaseCritRate() > 0 && Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
            context.block = Formulas.calcShldUse(caster, target, this, context.isCritical);
            context.value = Formulas.calcPhysicalSkillDamage(caster, target, this, context.block, context.isCritical, ss);

            if (context.value > 0) {
                caster.sendDamageMessage(target, (int) context.value, false, context.isCritical, false);

                // Possibility of a lethal strike
                Formulas.calcLethalHit(caster, target, this);

                // vengeance reflected damage
                context.isAvenged = isAvenged(caster, target);
                if (context.isAvenged) {
                    caster.reduceCurrentHp(context.value, target, this);
                } else {
                    target.reduceCurrentHp(context.value, caster, this);
                }

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

            } else {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
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