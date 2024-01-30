package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;

public class ManaHeal extends Default {

    public ManaHeal(StatSet set) {
        super(set);
    }

    @Override
    public boolean isHeal() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (!target.canBeHealed()) {
                continue;
            }

            Default.Context context = Default.Context.builder().build();
            double mpRestore = getPower();

            if (getSkillType() == SkillType.MANAHEAL_PERCENT) {
                mpRestore = target.getStatus().getMaxMp() * mpRestore / 100.0;
            } else {
                mpRestore = (getSkillType() == SkillType.MANARECHARGE) ? target.getStatus().calcStat(Stats.RECHARGE_MP_RATE, mpRestore, null, null) : mpRestore;
            }

            mpRestore = target.getStatus().addMp(mpRestore);

            context.value = mpRestore;

            if (caster instanceof Player && caster != target) {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_MP_RESTORED_BY_S1).addCharName(caster).addNumber((int) mpRestore));
            } else {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_MP_RESTORED).addNumber((int) mpRestore));
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

        if (!isPotion()) {
            caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
        }
    }
}