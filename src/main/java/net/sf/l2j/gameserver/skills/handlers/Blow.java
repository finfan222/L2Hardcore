package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.Map;

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
            if (!(obj instanceof Creature)) {
                continue;
            }

            final Creature target = ((Creature) obj);
            if (target.isAlikeDead()) {
                continue;
            }

            if (Formulas.calcBlowRate(caster, target, this)) {
                // Calculate skill evasion.
                if (Formulas.calcPhysicalSkillEvasion(target, this)) {
                    if (caster instanceof Player) {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
                    }

                    if (target instanceof Player) {
                        target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(caster));
                    }

                    continue;
                }

                final boolean isCrit = getBaseCritRate() > 0 && Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
                final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, isCrit);

                // Calculate skill reflect
                final byte reflect = Formulas.calcSkillReflect(target, this);
                if (this.hasEffects()) {
                    if (reflect == Formulas.SKILL_REFLECT_SUCCEED) {
                        caster.stopSkillEffects(this.getId());
                        this.applyEffects(target, caster);
                    } else {
                        target.stopSkillEffects(this.getId());
                        if (Formulas.calcSkillSuccess(caster, target, this, sDef, true)) {
                            this.applyEffects(caster, target, sDef, false);
                        } else {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                        }
                    }
                }

                double damage = (int) Formulas.calcBlowDamage(caster, target, this, sDef, ss);
                if (isCrit) {
                    damage *= 2;
                }

                target.reduceCurrentHp(damage, caster, this);

                // vengeance reflected damage
                if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
                    if (target instanceof Player) {
                        target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(caster));
                    }

                    if (caster instanceof Player) {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
                    }

                    // Formula from Diego post, 700 from rpg tests
                    double vegdamage = (700.0 * target.getStatus().getPAtk(caster)) / caster.getStatus().getPDef(target);
                    caster.reduceCurrentHp(vegdamage, target, this);
                }

                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, damage);

                // Send damage message.
                caster.sendDamageMessage(target, (int) damage, false, true, false);

                caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());

                notifyAboutSkillHit(caster, target, Map.of("damage", damage));
            }

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