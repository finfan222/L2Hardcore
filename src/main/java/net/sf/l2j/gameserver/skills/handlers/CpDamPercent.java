package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class CpDamPercent extends L2Skill {

    public CpDamPercent(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public boolean isDamage() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Player)) {
                continue;
            }

            final Player target = ((Player) obj);
            if (target.isDead() || target.isInvul()) {
                continue;
            }

            final ShieldDefense sDef = Formulas.calcShldUse(caster, target, this, false);

            int damage = (int) (target.getStatus().getCp() * (getPower() / 100));

            // Manage cast break of the target (calculating rate, sending message...)
            Formulas.calcCastBreak(target, damage);

            applyEffects(caster, target, sDef, bsps);
            caster.sendDamageMessage(target, damage, false, false, false);
            target.getStatus().setCp(target.getStatus().getCp() - damage);

            // Custom message to see Wrath damage on target
            target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addCharName(caster).addNumber(damage));
        }
        caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());
    }
}