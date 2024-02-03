package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Heal extends Default {

    public Heal(StatSet set) {
        super(set);
    }

    @Override
    public boolean isHeal() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        double power = getPower() + caster.getStatus().calcStat(Stats.HEAL_PROFICIENCY, 0, null, null);

        if (getSkillType() != SkillType.HEAL_STATIC) {
            boolean sps = caster.isChargedShot(ShotType.SPIRITSHOT);
            boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

            double staticShotBonus = 0;
            double mAtkMul = 1.;

            if ((sps || bsps) && (caster instanceof Player && caster.getActingPlayer().isMageClass()) || caster instanceof Summon) {
                staticShotBonus = getMpConsume(); // static bonus for spiritshots

                if (bsps) {
                    mAtkMul = 4.;
                    staticShotBonus *= 2.4;
                } else {
                    mAtkMul = 2.;
                }
            } else if ((sps || bsps) && caster instanceof Npc) {
                staticShotBonus = 2.4 * getMpConsume(); // always blessed spiritshots
                mAtkMul = 4.;
            } else {
                // shot dynamic bonus
                if (bsps) {
                    mAtkMul *= 4.;
                } else {
                    mAtkMul += 1.;
                }
            }

            power += staticShotBonus + Math.sqrt(mAtkMul * caster.getStatus().getMAtk(caster, null));

            if (!isPotion()) {
                caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
            }
        }

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (!target.canBeHealed()) {
                continue;
            }

            Context context = Context.builder().build();
            double amount = target.getStatus().addHp(power * target.getStatus().calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100.);

            // poison reduce healing potency
            if (target.isAffected(EffectFlag.POISON)) {
                amount *= 1 - Rnd.get(0, 30) / 100.;
            }

            context.value = amount;

            if (hasEffects()) {
                target.stopSkillEffects(getId());
                applyEffects(caster, target);
            }

            if (target instanceof Player) {
                if (getId() == 4051) {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.REJUVENATING_HP));
                } else {
                    if (caster != target) {
                        target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addCharName(caster).addNumber((int) amount));
                    } else {
                        target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED).addNumber((int) amount));
                    }
                }
            }

            notifyAboutSkillHit(caster, target, context);
        }
    }
}