package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class StriderSiegeAssault extends L2Skill {

    public StriderSiegeAssault(StatSet set) {
        super(set);
    }

    public static boolean check(Player player, WorldObject target, L2Skill skill) {
        SystemMessage sm = null;

        if (!player.isRiding()) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        } else if (!(target instanceof Door)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET);
        } else {
            final Siege siege = CastleManager.getInstance().getActiveSiege(player);
            if (siege == null || !siege.checkSide(player.getClan(), SiegeSide.ATTACKER)) {
                sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
            }
        }

        if (sm != null) {
            player.sendPacket(sm);
        }

        return sm == null;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        if (!check(player, targets[0], this)) {
            return;
        }

        final Door door = (Door) targets[0];
        if (door.isAlikeDead()) {
            return;
        }

        final boolean isCrit = Formulas.calcCrit(caster, door, this);
        final boolean ss = caster.isChargedShot(ShotType.SOULSHOT);
        final ShieldDefense sDef = Formulas.calcShldUse(caster, door, this, isCrit);

        final int damage = (int) Formulas.calcPhysicalSkillDamage(caster, door, this, sDef, isCrit, ss);
        if (damage > 0) {
            caster.sendDamageMessage(door, damage, false, false, false);
            door.reduceCurrentHp(damage, caster, this);
        } else {
            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
        }

        caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());
    }
}