package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Disablers extends Default {

    public Disablers(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return switch (getSkillType()) {
            case BETRAY, AGGDAMAGE, STUN, ROOT, CONFUSION, ERASE, SLEEP, MUTE, PARALYZE, AGGREDUCE, AGGREDUCE_CHAR, AGGREMOVE ->
                true;
            default -> false;
        };
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final SkillType type = getSkillType();

        final boolean bsps = caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isDead() || (target.isInvul() && !target.isParalyzed())) // bypass if target is dead or invul (excluding invul from Petrification)
            {
                continue;
            }

            if (isOffensive() && target.getFirstEffect(EffectType.BLOCK_DEBUFF) != null) {
                continue;
            }

            Context context = Context.builder().build();

            context.block = Formulas.calcShldUse(caster, target, this, false);
            context.isSuccess = Formulas.calcSkillSuccess(caster, target, this, context.block, bsps);

            switch (type) {
                case BETRAY:
                    if (context.isSuccess) {
                        applyEffects(caster, target, context.block, bsps);
                    } else {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                    }
                    break;

                case FAKE_DEATH:
                    // stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
                    applyEffects(caster, target, context.block, bsps);
                    break;

                case ROOT:
                case STUN:
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        target = caster;
                    }

                    if (context.isSuccess) {
                        applyEffects(caster, target, context.block, bsps);
                    } else {
                        if (caster instanceof Player) {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
                        }
                    }
                    break;

                case SLEEP:
                case PARALYZE: // use same as root for now
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        target = caster;
                    }

                    if (context.isSuccess) {
                        applyEffects(caster, target, context.block, bsps);
                    } else {
                        if (caster instanceof Player) {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
                        }
                    }
                    break;

                case MUTE:
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        target = caster;
                    }

                    if (context.isSuccess) {
                        // stop same type effect if available
                        for (AbstractEffect effect : target.getAllEffects()) {
                            if (effect.getTemplate().getStackOrder() == 99) {
                                continue;
                            }

                            if (effect.getSkill().getSkillType() == type) {
                                effect.exit();
                            }
                        }
                        applyEffects(caster, target, context.block, bsps);
                    } else {
                        if (caster instanceof Player) {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
                        }
                    }
                    break;

                case CONFUSION:
                    // do nothing if not on mob
                    if (target instanceof Attackable || target instanceof Playable) {
                        if (context.isSuccess) {
                            for (AbstractEffect effect : target.getAllEffects()) {
                                if (effect.getTemplate().getStackOrder() == 99) {
                                    continue;
                                }

                                if (effect.getSkill().getSkillType() == type) {
                                    effect.exit();
                                }
                            }
                            applyEffects(caster, target, context.block, bsps);
                        } else {
                            if (caster instanceof Player) {
                                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                            }
                        }
                    } else {
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVALID_TARGET));
                    }
                    break;

                case AGGDAMAGE:
                    if (target instanceof Attackable) {
                        target.getAI().notifyEvent(AiEventType.AGGRESSION, caster, (int) (getPower() / (target.getStatus().getLevel() + 7) * 150));
                    }

                    applyEffects(caster, target, context.block, bsps);
                    break;

                case AGGREDUCE:
                    // TODO these skills needs to be rechecked
                    if (target instanceof Attackable) {
                        applyEffects(caster, target, context.block, bsps);

                        if (getPower() > 0) {
                            ((Attackable) target).getAggroList().reduceAllHate((int) getPower());
                        } else {
                            final int hate = ((Attackable) target).getAggroList().getHate(caster);
                            final double diff = hate - target.getStatus().calcStat(Stats.AGGRESSION, hate, target, this);
                            if (diff > 0) {
                                ((Attackable) target).getAggroList().reduceAllHate((int) diff);
                            }
                        }
                    }
                    break;

                case AGGREDUCE_CHAR:
                    // TODO these skills need to be rechecked
                    if (context.isSuccess) {
                        if (target instanceof Attackable) {
                            ((Attackable) target).getAggroList().stopHate(caster);
                        }

                        applyEffects(caster, target, context.block, bsps);
                    } else {
                        if (caster instanceof Player) {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                        }
                    }
                    break;

                case AGGREMOVE:
                    // TODO these skills needs to be rechecked
                    if (target instanceof Attackable && !target.isRaidRelated()) {
                        if (context.isSuccess) {
                            if (getTargetType() == SkillTargetType.UNDEAD) {
                                if (target.isUndead()) {
                                    ((Attackable) target).getAggroList().stopHate(caster);
                                }
                            } else {
                                ((Attackable) target).getAggroList().stopHate(caster);
                            }
                        } else {
                            if (caster instanceof Player) {
                                caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                            }
                        }
                    }
                    break;

                case ERASE:
                    // doesn't affect siege summons
                    if (context.isSuccess && !(target instanceof SiegeSummon)) {
                        final Player summonOwner = ((Summon) target).getOwner();
                        final Summon summonPet = summonOwner.getSummon();
                        if (summonPet != null) {
                            summonPet.unSummon(summonOwner);
                            summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
                        }
                    } else {
                        if (caster instanceof Player) {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                        }
                    }
                    break;

                case CANCEL_DEBUFF:
                    final AbstractEffect[] effects = target.getAllEffects();
                    if (effects == null || effects.length == 0) {
                        break;
                    }

                    int count = (getMaxNegatedEffects() > 0) ? 0 : -2;
                    for (AbstractEffect effect : effects) {
                        if (!effect.getSkill().isDebuff() || !effect.getSkill().canBeDispeled() || effect.getTemplate().getStackOrder() == 99) {
                            continue;
                        }

                        effect.exit();

                        if (count > -1) {
                            count++;
                            if (count >= getMaxNegatedEffects()) {
                                break;
                            }
                        }
                    }
                    break;

                case NEGATE:
                    context.isReflected = isReflected(caster, target, context.block);
                    if (context.isReflected) {
                        target = caster;
                    }

                    // Skills with negateId (skillId)
                    if (getNegateId().length != 0) {
                        for (int id : getNegateId()) {
                            if (id != 0) {
                                target.stopSkillEffects(id);
                            }
                        }
                    }
                    // All others negate type skills
                    else {
                        for (AbstractEffect effect : target.getAllEffects()) {
                            if (effect.getTemplate().getStackOrder() == 99) {
                                continue;
                            }

                            final L2Skill effectSkill = effect.getSkill();
                            for (SkillType skillType : getNegateStats()) {
                                // If power is -1 the effect is always removed without lvl check
                                if (getNegateLvl() == -1) {
                                    if (effect.getSkill().getSkillType() == skillType) {
                                        effect.exit();
                                    }
                                }
                                // Remove the effect according to its power.
                                else {
                                    if (effect.getSkill().getEffectAbnormalLvl() >= 0) {
                                        if (effect.getEffectSkillType() == skillType && effect.getSkill().getEffectAbnormalLvl() <= getNegateLvl()) {
                                            effect.exit();
                                        }
                                    } else if (effect.getSkill().getSkillType() == skillType && effect.getSkill().getAbnormalLvl() <= getNegateLvl()) {
                                        effect.exit();
                                    }
                                }
                            }
                        }
                    }
                    context.value = context.isSuccess ? (int) Formulas.calcNegateSkillPower(this, caster, target) : 0;
                    applyEffects(caster, target, context.block, bsps);
                    break;
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
        caster.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }
}