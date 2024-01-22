package net.sf.l2j.gameserver.enums.skills;

import net.sf.l2j.commons.data.StatSet;
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
    PDAM(Pdam.class),
    FATAL(Pdam.class),
    MDAM(Mdam.class),
    CPDAMPERCENT(CpDamPercent.class),
    MANADAM(Manadam.class),
    DOT(Continuous.class),
    MDOT(Continuous.class),
    DRAIN_SOUL(DrainSoul.class),
    DRAIN(Drain.class),
    DEATHLINK(Mdam.class),
    BLOW(Blow.class),
    SIGNET(Signet.class),
    SIGNET_CASTTIME(SignetCasttime.class),
    SEED(Seed.class),

    // Disablers
    BLEED(Continuous.class),
    POISON(Continuous.class),
    STUN(Disablers.class),
    ROOT(Disablers.class),
    CONFUSION(Disablers.class),
    FEAR(Continuous.class),
    SLEEP(Disablers.class),
    MUTE(Disablers.class),
    PARALYZE(Disablers.class),
    WEAKNESS(Continuous.class),

    // hp, mp, cp
    HEAL(Heal.class),
    MANAHEAL(ManaHeal.class),
    COMBATPOINTHEAL(CombatPointHeal.class),
    HOT(Continuous.class),
    MPHOT(Continuous.class),
    BALANCE_LIFE(BalanceLife.class),
    HEAL_STATIC(Heal.class),
    MANARECHARGE(ManaHeal.class),
    HEAL_PERCENT(HealPercent.class),
    MANAHEAL_PERCENT(HealPercent.class),

    GIVE_SP(GiveSp.class),

    // Aggro
    AGGDAMAGE(Disablers.class),
    AGGREDUCE(Disablers.class),
    AGGREMOVE(Disablers.class),
    AGGREDUCE_CHAR(Disablers.class),
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
    ERASE(Disablers.class),
    BETRAY(Disablers.class),
    SPAWN(Spawn.class),

    // Cancel
    CANCEL(Cancel.class),
    MAGE_BANE(Cancel.class),
    WARRIOR_BANE(Cancel.class),

    NEGATE(Disablers.class),
    CANCEL_DEBUFF(Disablers.class),

    BUFF(Continuous.class),
    DEBUFF(Continuous.class),
    PASSIVE,
    CONT(Continuous.class),

    RESURRECT(Resurrect.class),
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

    private SkillType() {
        _class = Default.class;
    }

    private SkillType(Class<? extends L2Skill> classType) {
        _class = classType;
    }
}