package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.DuelManager;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.ClanHallManagerNpc;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectFear;

public class Continuous extends Default {

    public Continuous(StatSet set) {
        super(set);
    }

    @Override
    public boolean isHeal() {
        return getSkillType() == SkillType.HOT || getSkillType() == SkillType.MPHOT;
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return switch (getSkillType()) {
            case DOT, BLEED, POISON, DEBUFF, AGGDEBUFF, FEAR, MDOT, WEAKNESS -> true;
            default -> false;
        };
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final Player player = caster.getActingPlayer();

        L2Skill skill = this;
        if (getEffectId() != 0) {
            L2Skill effectSkill = SkillTable.getInstance().getInfo(getEffectId(), getEffectLvl() == 0 ? 1 : getEffectLvl());
            if (effectSkill != null) {
                skill = effectSkill;
            }
        }

        final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            Context context = Context.builder().build();

            context.isReflected = isReflected(caster, target, ShieldDefense.FAILED);
            if (context.isReflected) {
                target = caster;
            }

            switch (getSkillType()) {
                case BUFF:
                    // Target under buff immunity.
                    if (target.getFirstEffect(EffectType.BLOCK_BUFF) != null) {
                        continue;
                    }

                    // Player holding a cursed weapon can't be buffed and can't buff
                    if (!(caster instanceof ClanHallManagerNpc) && target != caster) {
                        if (target instanceof Player) {
                            if (((Player) target).isCursedWeaponEquipped()) {
                                continue;
                            }
                        } else if (player != null && player.isCursedWeaponEquipped()) {
                            continue;
                        }
                    }
                    break;

                case HOT:
                case MPHOT:
                    if (caster.isInvul()) {
                        continue;
                    }
                    break;
                case FEAR:
                    if (target instanceof Playable && ArraysUtil.contains(EffectFear.DOESNT_AFFECT_PLAYABLE, skill.getId())) {
                        continue;
                    }
            }

            // Target under debuff immunity.
            if (skill.isOffensive() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) != null) {
                continue;
            }

            if (skill.isOffensive() || skill.isDebuff()) {
                context.block = Formulas.calcShldUse(caster, target, skill, false);
                context.isSuccess = Formulas.calcSkillSuccess(caster, target, skill, context.block, bsps);
            }

            if (context.isSuccess) {
                // if this is a debuff let the duel manager know about it so the debuff
                // can be removed after the duel (player & target must be in the same duel)
                if (target instanceof Player && ((Player) target).isInDuel() && (skill.getSkillType() == SkillType.DEBUFF || skill.getSkillType() == SkillType.BUFF) && player != null && player.getDuelId() == ((Player) target).getDuelId()) {
                    for (AbstractEffect buff : skill.applyEffects(caster, target, context.block, bsps)) {
                        if (buff != null) {
                            DuelManager.getInstance().onBuff(((Player) target), buff);
                        }
                    }
                } else {
                    skill.applyEffects(caster, target, context.block, bsps);
                }

                if (skill.getSkillType() == SkillType.AGGDEBUFF) {
                    if (target instanceof Attackable) {
                        target.getAI().notifyEvent(AiEventType.AGGRESSION, caster, (int) skill.getPower());
                    } else if (target instanceof Playable) {
                        if (target.getTarget() == caster) {
                            target.getAI().tryToAttack(caster, false, false);
                        } else {
                            target.setTarget(caster);
                        }
                    }
                }
            } else {
                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
            }

            // Possibility of a lethal strike
            Formulas.calcLethalHit(caster, target, skill);

            notifyAboutSkillHit(caster, target, context);
        }

        if (skill.hasSelfEffects()) {
            final AbstractEffect effect = caster.getFirstEffect(skill.getId());
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }

            skill.applySelfEffects(caster);
        }

        if (!skill.isPotion() && !skill.isToggle()) {
            caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
        }
    }
}