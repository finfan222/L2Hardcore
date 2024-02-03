package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Unlock extends Default {

    public Unlock(StatSet set) {
        super(set);
    }

    private static boolean doorUnlock(L2Skill skill) {
        if (skill.getSkillType() == SkillType.UNLOCK_SPECIAL) {
            return Rnd.get(100) < skill.getPower();
        }

        return switch (skill.getLevel()) {
            case 0 -> false;
            case 1 -> Rnd.get(120) < 30;
            case 2 -> Rnd.get(120) < 50;
            case 3 -> Rnd.get(120) < 75;
            default -> Rnd.get(120) < 100;
        };
    }

    private static boolean chestUnlock(L2Skill skill, int level) {
        int chance = 0;
        if (level > 60) {
            if (skill.getLevel() < 10) {
                return false;
            }

            chance = (skill.getLevel() - 10) * 5 + 30;
        } else if (level > 40) {
            if (skill.getLevel() < 6) {
                return false;
            }

            chance = (skill.getLevel() - 6) * 5 + 10;
        } else if (level > 30) {
            if (skill.getLevel() < 3) {
                return false;
            }

            if (skill.getLevel() > 12) {
                return true;
            }

            chance = (skill.getLevel() - 3) * 5 + 30;
        } else {
            if (skill.getLevel() > 10) {
                return true;
            }

            chance = skill.getLevel() * 5 + 35;
        }

        chance = Math.min(chance, 50);
        return Rnd.get(100) < chance;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final WorldObject object = targets[0];
        Context context = Context.builder().build();

        if (object instanceof Door door) {
            if (!door.isUnlockable() && getSkillType() != SkillType.UNLOCK_SPECIAL) {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UNABLE_TO_UNLOCK_DOOR));
                return;
            }

            if (doorUnlock(this) && (!door.isOpened())) {
                door.openMe();
            } else {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
            }

            context.value = Math.min(Formulas.calcNegateSkillPower(this, caster, door), 10_000);
            notifyAboutSkillHit(caster, door, context);
        } else if (object instanceof Chest chest) {
            if (chest.isDead() || chest.isInteracted()) {
                return;
            }

            chest.setInteracted();
            if (chestUnlock(this, chest.getStatus().getLevel())) {
                chest.setSpecialDrop();
                chest.doDie(chest);

            } else {
                chest.getAggroList().addDamageHate(caster, 0, 200);
                chest.getAI().tryToAttack(caster);
            }

            context.value = Math.min(Formulas.calcNegateSkillPower(this, caster, chest), 10_000);
            notifyAboutSkillHit(caster, chest, context);
        } else {
            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET));
        }
    }
}