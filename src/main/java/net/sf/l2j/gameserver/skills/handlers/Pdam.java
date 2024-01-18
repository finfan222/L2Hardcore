package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.effects.EffectFear;

import java.util.Map;

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

        final ItemInstance weapon = caster.getActiveWeaponInstance();

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature)) {
                continue;
            }

            final Creature target = ((Creature) obj);
            if (target.isDead()) {
                continue;
            }

            if (target instanceof Playable && ArraysUtil.contains(EffectFear.DOESNT_AFFECT_PLAYABLE, getId())) {
                continue;
            }

            // Calculate skill evasion. As Dodge blocks only melee skills, make an exception with bow weapons.
            if (weapon != null && weapon.getItemType() != WeaponType.BOW && Formulas.calcPhysicalSkillEvasion(target, this)) {
                if (caster instanceof Player) {
                    caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
                }

                if (target instanceof Player) {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(caster));
                }

                // no futher calculations needed.
                continue;
            }

            final boolean isCrit = getBaseCritRate() > 0 && Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
            final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, isCrit);
            final byte reflect = Formulas.calcSkillReflect(target, this);

            if (hasEffects() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) == null) {
                if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0) {
                    caster.stopSkillEffects(getId());

                    applyEffects(target, caster);
                } else {
                    target.stopSkillEffects(getId());

                    applyEffects(caster, target, sDef, false);
                }
            }

            final int damage = (int) Formulas.calcPhysicalSkillDamage(caster, target, this, sDef, isCrit, ss);
            if (damage > 0) {
                caster.sendDamageMessage(target, damage, false, isCrit, false);

                // Possibility of a lethal strike
                Formulas.calcLethalHit(caster, target, this);

                target.reduceCurrentHp(damage, caster, this);

                // vengeance reflected damage
                if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
                    if (target instanceof Player) {
                        target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(caster));
                    }

                    if (caster instanceof Player) {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
                    }

                    double vegdamage = (700. * target.getStatus().getPAtk(caster) / caster.getStatus().getPDef(target));
                    caster.reduceCurrentHp(vegdamage, target, this);
                }
            } else {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
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