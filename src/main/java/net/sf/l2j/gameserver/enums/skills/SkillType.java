package net.sf.l2j.gameserver.enums.skills;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.handlers.Appearance;
import net.sf.l2j.gameserver.skills.handlers.BalanceLife;
import net.sf.l2j.gameserver.skills.handlers.Blow;
import net.sf.l2j.gameserver.skills.handlers.Cancel;
import net.sf.l2j.gameserver.skills.handlers.ChargeDmg;
import net.sf.l2j.gameserver.skills.handlers.CombatPointHeal;
import net.sf.l2j.gameserver.skills.handlers.Continuous;
import net.sf.l2j.gameserver.skills.handlers.CpDamPercent;
import net.sf.l2j.gameserver.skills.handlers.Craft;
import net.sf.l2j.gameserver.skills.handlers.CreateItem;
import net.sf.l2j.gameserver.skills.handlers.Default;
import net.sf.l2j.gameserver.skills.handlers.Disablers;
import net.sf.l2j.gameserver.skills.handlers.Drain;
import net.sf.l2j.gameserver.skills.handlers.DrainSoul;
import net.sf.l2j.gameserver.skills.handlers.Dummy;
import net.sf.l2j.gameserver.skills.handlers.Extractable;
import net.sf.l2j.gameserver.skills.handlers.Fishing;
import net.sf.l2j.gameserver.skills.handlers.FishingSkill;
import net.sf.l2j.gameserver.skills.handlers.GetPlayer;
import net.sf.l2j.gameserver.skills.handlers.GiveSp;
import net.sf.l2j.gameserver.skills.handlers.Harvest;
import net.sf.l2j.gameserver.skills.handlers.Heal;
import net.sf.l2j.gameserver.skills.handlers.HealPercent;
import net.sf.l2j.gameserver.skills.handlers.InstantJump;
import net.sf.l2j.gameserver.skills.handlers.ManaHeal;
import net.sf.l2j.gameserver.skills.handlers.Manadam;
import net.sf.l2j.gameserver.skills.handlers.Mdam;
import net.sf.l2j.gameserver.skills.handlers.Pdam;
import net.sf.l2j.gameserver.skills.handlers.Resurrect;
import net.sf.l2j.gameserver.skills.handlers.Seed;
import net.sf.l2j.gameserver.skills.handlers.Shot;
import net.sf.l2j.gameserver.skills.handlers.SiegeFlag;
import net.sf.l2j.gameserver.skills.handlers.Signet;
import net.sf.l2j.gameserver.skills.handlers.SignetCasttime;
import net.sf.l2j.gameserver.skills.handlers.Sow;
import net.sf.l2j.gameserver.skills.handlers.Spawn;
import net.sf.l2j.gameserver.skills.handlers.Spoil;
import net.sf.l2j.gameserver.skills.handlers.StriderSiegeAssault;
import net.sf.l2j.gameserver.skills.handlers.SummonCreature;
import net.sf.l2j.gameserver.skills.handlers.SummonFriend;
import net.sf.l2j.gameserver.skills.handlers.SummonServitor;
import net.sf.l2j.gameserver.skills.handlers.Sweep;
import net.sf.l2j.gameserver.skills.handlers.TakeCastle;
import net.sf.l2j.gameserver.skills.handlers.Teleport;
import net.sf.l2j.gameserver.skills.handlers.Unlock;

import java.lang.reflect.Constructor;

public enum SkillType {
    // Damage
    PDAM(Pdam.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            if (!(target instanceof Monster monster)) {
                return;
            }

            int spReward = monster.getSpReward();
            if (spReward > 0) {
                int diff = caster.getStatus().getLevel() - monster.getStatus().getLevel() - 5;
                double pow = Math.pow(0.8333, diff);
                if (skill != null) {
                    double coefficient = Math.min(value / monster.getStatus().getMaxHp(), 1.0);
                    int sp = (int) (coefficient * (spReward * pow));
                    caster.getActingPlayer().addSp(sp);
                    caster.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP).addNumber(sp));
                }
            }
        }
    },
    FATAL(Pdam.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            PDAM.rewardSp(caster, target, skill, value);
        }
    },
    MDAM(Mdam.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            PDAM.rewardSp(caster, target, skill, value);
        }
    },
    CPDAMPERCENT(CpDamPercent.class),
    MANADAM(Manadam.class),
    DOT(Continuous.class),
    MDOT(Continuous.class),
    DRAIN_SOUL(DrainSoul.class),
    DRAIN(Drain.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            if (target.isDead()) {
                return;
            }

            PDAM.rewardSp(caster, target, skill, value);
        }
    },
    DEATHLINK(Mdam.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            PDAM.rewardSp(caster, target, skill, value);
        }
    },
    BLOW(Blow.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            PDAM.rewardSp(caster, target, skill, value);
        }
    },
    SIGNET(Signet.class),
    SIGNET_CASTTIME(SignetCasttime.class),
    SEED(Seed.class),

    // Disablers
    BLEED(Continuous.class),
    POISON(Continuous.class),
    STUN(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            if (!(target instanceof Monster monster)) {
                return;
            }

            if (monster.isDead()) {
                return;
            }

            int spReward = monster.getSpReward();
            if (spReward > 0) {
                int diff = caster.getStatus().getLevel() - monster.getStatus().getLevel() - 5;
                double pow = Math.pow(0.8333, diff);
                int coefficient = skill.getMagicLevel() > 0 ? skill.getMagicLevel() * 10 : caster.getStatus().getLevel() * 10;
                int sp = (int) (coefficient * (spReward * pow));
                caster.getActingPlayer().addSp(sp);
                caster.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP).addNumber(sp));
            }
        }
    },
    ROOT(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    CONFUSION(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    FEAR(Continuous.class),
    SLEEP(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    MUTE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    PARALYZE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    WEAKNESS(Continuous.class),

    // hp, mp, cp
    HEAL(Heal.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            if (target instanceof Playable playable) {
                if (playable.isAttackingByMonsters() && value > 0) {
                    //(MAGIC LEVEL ÷ MONSTER LEVEL) × (HEAL AMOUNT⁣ ÷ 9999) × SP REWARD
                    int magicLevel = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : target.getStatus().getLevel();
                    double monsterLevel = target.getStatus().getLevel();
                    double healAmount = value / 9999.;
                    int sp = (int) ((magicLevel / monsterLevel) * (healAmount / 9999.) * playable.getFirstMonsterAttacker().getSpReward());
                    Player healer = caster.getActingPlayer();
                    if (!healer.isDead()) {
                        caster.getActingPlayer().addSp(sp);
                        caster.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP).addNumber(sp));
                    }
                }
            }
        }
    },
    MANAHEAL(ManaHeal.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            HEAL.rewardSp(caster, target, skill, value);
        }
    },
    COMBATPOINTHEAL(CombatPointHeal.class),
    HOT(Continuous.class),
    MPHOT(Continuous.class),
    BALANCE_LIFE(BalanceLife.class),
    HEAL_STATIC(Heal.class),
    MANARECHARGE(ManaHeal.class),
    HEAL_PERCENT(HealPercent.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            HEAL.rewardSp(caster, target, skill, value);
        }
    },
    MANAHEAL_PERCENT(HealPercent.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            HEAL.rewardSp(caster, target, skill, value);
        }
    },

    GIVE_SP(GiveSp.class),

    // Aggro
    AGGDAMAGE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    AGGREDUCE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    AGGREMOVE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    AGGREDUCE_CHAR(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    AGGDEBUFF(Continuous.class),

    // Fishing
    FISHING(Fishing.class),
    PUMPING(FishingSkill.class),
    REELING(FishingSkill.class),

    // MISC
    UNLOCK(Unlock.class),
    UNLOCK_SPECIAL(Unlock.class),
    DELUXE_KEY_UNLOCK(Dummy.class),
    ENCHANT_ARMOR,
    ENCHANT_WEAPON,
    SOULSHOT(Shot.class),
    SPIRITSHOT(Shot.class),
    SIEGE_FLAG(SiegeFlag.class),
    TAKE_CASTLE(TakeCastle.class),
    SOW(Sow.class),
    HARVEST(Harvest.class),
    GET_PLAYER(GetPlayer.class),
    DUMMY(Dummy.class),
    INSTANT_JUMP(InstantJump.class),

    // Creation
    COMMON_CRAFT(Craft.class),
    DWARVEN_CRAFT(Craft.class),
    CREATE_ITEM(CreateItem.class),
    EXTRACTABLE(Extractable.class),
    EXTRACTABLE_FISH(Extractable.class),

    // Summons
    SUMMON(SummonServitor.class),
    FEED_PET,
    STRIDER_SIEGE_ASSAULT(StriderSiegeAssault.class),
    ERASE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    BETRAY(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    SPAWN(Spawn.class),

    // Cancel
    CANCEL(Cancel.class),
    MAGE_BANE(Cancel.class),
    WARRIOR_BANE(Cancel.class),

    NEGATE(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },
    CANCEL_DEBUFF(Disablers.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            STUN.rewardSp(caster, target, skill, value);
        }
    },

    BUFF(Continuous.class),
    DEBUFF(Continuous.class),
    PASSIVE,
    CONT(Continuous.class),

    RESURRECT(Resurrect.class) {
        @Override
        public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
            int sp = (int) (Math.pow(skill.getLevel(), 3) * 100.);
            caster.getActingPlayer().addSp(sp);
            caster.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP).addNumber(sp));
        }
    },
    CHARGEDAM(ChargeDmg.class),
    LUCK,
    RECALL(Teleport.class),
    TELEPORT(Teleport.class),
    SUMMON_FRIEND(SummonFriend.class),
    SUMMON_PARTY(SummonFriend.class),
    SUMMON_CREATURE(SummonCreature.class),
    REFLECT(Continuous.class),
    SPOIL(Spoil.class),
    SWEEP(Sweep.class),
    FAKE_DEATH(Disablers.class),
    BEAST_FEED(Dummy.class),
    FUSION(Continuous.class),

    CHANGE_APPEARANCE(Appearance.class),

    // Skill is done within the core.
    COREDONE,

    // unimplemented
    NOTDONE;

    private final Class<? extends L2Skill> _class;

    public L2Skill makeSkill(StatSet set) {
        try {
            Constructor<? extends L2Skill> c = _class.getConstructor(StatSet.class);

            return c.newInstance(set);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    SkillType() {
        _class = Default.class;
    }

    SkillType(Class<? extends L2Skill> classType) {
        _class = classType;
    }

    public void rewardSp(Playable caster, Creature target, L2Skill skill, double value) {
    }

    public void fractureArmor(Player target, L2Skill skill, boolean isMissed, ShieldDefense block, double damage) {
    }
}