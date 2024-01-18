package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.Map;

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

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature)) {
                continue;
            }

            final Creature target = ((Creature) obj);
            if (target.isAlikeDead() && getTargetType() != SkillTargetType.CORPSE_MOB) {
                continue;
            }

            if (caster != target && target.isInvul()) {
                continue; // No effect on invulnerable chars unless they cast it themselves.
            }

            final boolean isCrit = Formulas.calcMCrit(caster, target, this);
            final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, false);
            final int damage = (int) Formulas.calcMagicDam(caster, target, this, sDef, sps, bsps, isCrit);

            if (damage > 0) {
                int targetCp = 0;
                if (target instanceof Player) {
                    targetCp = (int) ((Player) target).getStatus().getCp();
                }

                final int targetHp = (int) target.getStatus().getHp();

                int drain = 0;
                if (isPlayable && targetCp > 0) {
                    if (damage < targetCp) {
                        drain = 0;
                    } else {
                        drain = damage - targetCp;
                    }
                } else if (damage > targetHp) {
                    drain = targetHp;
                } else {
                    drain = damage;
                }

                caster.getStatus().addHp(_absorbAbs + _absorbPart * drain);

                // That section is launched for drain skills made on ALIVE targets.
                if (!target.isDead() || getTargetType() != SkillTargetType.CORPSE_MOB) {
                    // Manage cast break of the target (calculating rate, sending message...)
                    Formulas.calcCastBreak(target, damage);

                    caster.sendDamageMessage(target, damage, isCrit, false, false);

                    if (hasEffects() && getTargetType() != SkillTargetType.CORPSE_MOB) {
                        // ignoring vengance-like reflections
                        if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_SUCCEED) > 0) {
                            caster.stopSkillEffects(getId());
                            applyEffects(target, caster);
                        } else {
                            // activate attacked effects, if any
                            target.stopSkillEffects(getId());
                            if (Formulas.calcSkillSuccess(caster, target, this, sDef, bsps)) {
                                applyEffects(caster, target);
                            } else {
                                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
                            }
                        }
                    }
                    target.reduceCurrentHp(damage, caster, this);
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