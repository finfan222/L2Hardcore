package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class HealPercent extends Default {

    public HealPercent(StatSet set) {
        super(set);
    }

    @Override
    public boolean isHeal() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final boolean isHp = getSkillType() == SkillType.HEAL_PERCENT;

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (!target.canBeHealed()) {
                continue;
            }

            Context context = Context.builder().build();
            double amount;
            if (isHp) {
                double hp = target.getStatus().getMaxHp() * getPower() / 100.;
                if (target.isAffected(EffectFlag.POISON)) {
                    hp *= 1 - Rnd.get(0, 30) / 100.;
                }
                amount = target.getStatus().addHp(hp);
            } else {
                double mp = target.getStatus().getMaxMp() * getPower() / 100.;
                if (target.isAffected(EffectFlag.POISON)) {
                    mp *= 1 - Rnd.get(0, 30) / 100.;
                }
                amount = target.getStatus().addMp(mp);
            }

            context.value = amount;

            if (hasEffects()) {
                target.stopSkillEffects(getId());
                applyEffects(caster, target);
            }

            if (target instanceof Player) {
                SystemMessage sm;
                if (isHp) {
                    if (caster != target) {
                        sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addCharName(caster);
                    } else {
                        sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED);
                    }
                } else {
                    if (caster != target) {
                        sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MP_RESTORED_BY_S1).addCharName(caster);
                    } else {
                        sm = SystemMessage.getSystemMessage(SystemMessageId.S1_MP_RESTORED);
                    }
                }
                sm.addNumber((int) amount);
                target.sendPacket(sm);
            }

            notifyAboutSkillHit(caster, target, context);
        }
    }
}