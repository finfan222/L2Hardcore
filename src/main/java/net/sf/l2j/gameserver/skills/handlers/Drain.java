package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

public class Drain extends Default {
    private final float _absorbPart;
    private final int _absorbAbs;

    public Drain(StatSet set) {
        super(set);

        _absorbPart = set.getFloat("absorbPart", 0.f);
        _absorbAbs = set.getInteger("absorbAbs", 0);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        final boolean sps = caster.isChargedShot(ShotType.SPIRITSHOT);
        final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
        final boolean isPlayable = caster instanceof Playable;

        Context context = Context.builder().build();

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isAlikeDead() && getTargetType() != SkillTargetType.CORPSE_MOB) {
                continue;
            }

            if (caster != target && target.isInvul()) {
                continue; // No effect on invulnerable chars unless they cast it themselves.
            }

            context.isCritical = Formulas.calcMCrit(caster, target, this);
            context.block = Formulas.calcShldUse(caster, target, this, false);
            context.value = (int) Formulas.calcMagicDam(caster, target, this, context.block, sps, bsps, context.isCritical);

            if (context.value > 0) {
                int targetCp = 0;
                if (target instanceof Player player) {
                    targetCp = (int) player.getStatus().getCp();
                }

                final int targetHp = (int) target.getStatus().getHp();

                int drain = 0;
                if (isPlayable && targetCp > 0) {
                    if (context.value >= targetCp) {
                        drain = (int) (context.value - targetCp);
                    }
                } else {
                    drain = (int) Math.min(context.value, targetHp);
                }

                caster.getStatus().addHp(_absorbAbs + _absorbPart * drain);

                // That section is launched for drain skills made on ALIVE targets.
                if (!target.isDead() || getTargetType() != SkillTargetType.CORPSE_MOB) {
                    // Manage cast break of the target (calculating rate, sending message...)
                    Formulas.calcCastBreak(target, context.value);

                    caster.sendDamageMessage(target, (int) context.value, context.isCritical, false, false);

                    if (hasEffects() && getTargetType() != SkillTargetType.CORPSE_MOB) {
                        context.isReflected = isReflected(caster, target, context.block);
                        if (context.isReflected) {
                            caster.stopSkillEffects(this.getId());
                            applyEffects(target, caster);
                        } else {
                            target.stopSkillEffects(this.getId());
                            applyEffects(caster, target, context.block, false);
                        }
                    }
                    target.reduceCurrentHp(context.value, caster, this);
                }
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

        caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }

    public float getAbsorbPart() {
        return _absorbPart;
    }

    public int getAbsorbAbs() {
        return _absorbAbs;
    }

    @Override
    public boolean isDamage() {
        return true;
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }
}