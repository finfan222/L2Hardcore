package net.sf.l2j.gameserver.skills;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.PlayerLevelData;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.enums.actors.NpcRace;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.ElementType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.instance.SiegeFlag;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.handlers.Default;
import net.sf.l2j.gameserver.taskmanager.DayNightTaskManager;

@Slf4j
public final class Formulas {

    private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs

    public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
    public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
    public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both

    private static final byte MELEE_ATTACK_RANGE = 40;

    public static final int MAX_STAT_VALUE = 100;

    private static final double[] STR_COMPUTE = new double[]
        {
            1.036,
            34.845
        };
    private static final double[] INT_COMPUTE = new double[]
        {
            1.020,
            31.375
        };
    private static final double[] DEX_COMPUTE = new double[]
        {
            1.009,
            19.360
        };
    private static final double[] WIT_COMPUTE = new double[]
        {
            1.050,
            20.000
        };
    private static final double[] CON_COMPUTE = new double[]
        {
            1.030,
            27.632
        };
    private static final double[] MEN_COMPUTE = new double[]
        {
            1.010,
            -0.060
        };

    public static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
    public static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];
    public static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
    public static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
    public static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
    public static final double[] CON_BONUS = new double[MAX_STAT_VALUE];

    public static final double[] BASE_EVASION_ACCURACY = new double[MAX_STAT_VALUE];

    private static final double[] SQRT_MEN_BONUS = new double[MAX_STAT_VALUE];
    private static final double[] SQRT_CON_BONUS = new double[MAX_STAT_VALUE];

    static {
        for (int i = 0; i < STR_BONUS.length; i++) {
            STR_BONUS[i] = Math.floor(Math.pow(STR_COMPUTE[0], i - STR_COMPUTE[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < INT_BONUS.length; i++) {
            INT_BONUS[i] = Math.floor(Math.pow(INT_COMPUTE[0], i - INT_COMPUTE[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < DEX_BONUS.length; i++) {
            DEX_BONUS[i] = Math.floor(Math.pow(DEX_COMPUTE[0], i - DEX_COMPUTE[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < WIT_BONUS.length; i++) {
            WIT_BONUS[i] = Math.floor(Math.pow(WIT_COMPUTE[0], i - WIT_COMPUTE[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < CON_BONUS.length; i++) {
            CON_BONUS[i] = Math.floor(Math.pow(CON_COMPUTE[0], i - CON_COMPUTE[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < MEN_BONUS.length; i++) {
            MEN_BONUS[i] = Math.floor(Math.pow(MEN_COMPUTE[0], i - MEN_COMPUTE[1]) * 100 + .5d) / 100;
        }

        for (int i = 0; i < BASE_EVASION_ACCURACY.length; i++) {
            BASE_EVASION_ACCURACY[i] = Math.sqrt(i) * 6;
        }

        // Precompute square root values
        for (int i = 0; i < SQRT_CON_BONUS.length; i++) {
            SQRT_CON_BONUS[i] = Math.sqrt(CON_BONUS[i]);
        }
        for (int i = 0; i < SQRT_MEN_BONUS.length; i++) {
            SQRT_MEN_BONUS[i] = Math.sqrt(MEN_BONUS[i]);
        }
    }

    /**
     * @param creature : The {@link Creature} to test.
     * @return The period between 2 regenerations task.
     */
    public static int getRegeneratePeriod(Creature creature) {
        if (creature instanceof Door) {
            return HP_REGENERATE_PERIOD * 100; // 5 mins
        }

        return HP_REGENERATE_PERIOD; // 3s
    }

    /**
     * Calculate the blow success rate, based on {@link Creature} attacker, {@link Creature} target and
     * {@link L2Skill}.
     *
     * @param attacker : The {@link Creature} attacker.
     * @param target : The {@link Creature} target.
     * @param skill : The {@link L2Skill} to use.
     * @return True if successful, false otherwise.
     */
    public static boolean calcBlowRate(Creature attacker, Creature target, L2Skill skill) {
        double baseRate = skill.getBaseLandRate();
        if (baseRate == 0.0) {
            return false;
        }

        final boolean isAttackerInFrontofTarget = attacker.isInFrontOf(target);
        final boolean isBackstab = skill.getId() == 30;

        if (isAttackerInFrontofTarget && isBackstab) {
            if (Config.DEVELOPER) {
                StringUtil.printSection("Blow Rate");
                log.info("Final blow rate overriden by backstab under front target: 30 / 1000");
            }
            return 30 > Rnd.get(1000);
        }

        final double dexMul = DEX_BONUS[attacker.getStatus().getDEX()];
        final double blowMul = attacker.getStatus().calcStat(Stats.BLOW_RATE, 1, target, null);
        final double posMul = (attacker.isBehind(target)) ? 1.15 : ((isAttackerInFrontofTarget) ? 1. : 1.1);

        final double blowRate = baseRate * 2 * dexMul * blowMul * posMul;

        if (Config.DEVELOPER) {
            StringUtil.printSection("Blow Rate");
            log.info("Basic values: baseRate: {}", baseRate);
            log.info("Multipliers: dex: {}, blow: {}, pos: {}", dexMul, blowMul, posMul);
            log.info("Final blow rate: {} / 1000", blowRate);
        }

        return Math.min(blowRate, (isBackstab) ? 1000 : 800) > Rnd.get(1000);
    }

    /**
     * Calculate the lethal success rate, based on {@link Creature} attacker, {@link Creature} target and few
     * {@link L2Skill} parameters.
     *
     * @param attacker : The {@link Creature} attacker.
     * @param target : The {@link Creature} target.
     * @param baseRate : The base lethal chance of the {@link L2Skill}.
     * @param magiclvl : The associated magic {@link L2Skill} level.
     * @return True if successful, false otherwise.
     */
    private static boolean calcLethalRate(Creature attacker, Creature target, double baseRate, int magiclvl) {
        final double lethalMul = attacker.getStatus().calcStat(Stats.LETHAL_RATE, 1, target, null);

        final int attackerLevel = attacker.getStatus().getLevel();
        final int targetLevel = target.getStatus().getLevel();

        double editedRate;
        if (magiclvl > 0) {
            final int delta = ((magiclvl + attackerLevel) / 2) - 1 - targetLevel;
            if (delta >= -3) {
                editedRate = baseRate * attackerLevel / targetLevel;
            } else if (delta < -3 && delta >= -9) {
                editedRate = baseRate / delta * -3;
            } else {
                editedRate = baseRate / 15;
            }
        } else {
            editedRate = baseRate * attackerLevel / targetLevel;
        }

        final double lethalRate = editedRate * 10 * lethalMul;

        if (Config.DEVELOPER) {
            StringUtil.printSection("Lethal Rate");
            log.info("Basic values: baseRate: {}, editedRate: {}", baseRate, editedRate);
            log.info("Multipliers: lethal: {}", lethalMul);
            log.info("Final lethal rate: {} / 1000", lethalRate);
        }

        return lethalRate > Rnd.get(1000);
    }

    public static void calcLethalHit(Creature attacker, Creature target, L2Skill skill) {
        // If the attacker can't attack, return.
        final Player attackerPlayer = attacker.getActingPlayer();
        if (attackerPlayer != null && !attackerPlayer.getAccessLevel().canGiveDamage()) {
            return;
        }

        // If the target is invulnerable, related to RaidBoss or a Door/SiegeFlag, return.
        if (target.isInvul() || target.isRaidRelated() || target instanceof Door || target instanceof SiegeFlag) {
            return;
        }

        // If one of following IDs is found, return (Tyrannosaurus x 3, Headquarters).
        if (target instanceof Npc) {
            switch (((Npc) target).getNpcId()) {
                case 22215:
                case 22216:
                case 22217:
                case 35062:
                    return;
            }
        }

        // Second lethal effect (hp to 1 for npc, cp/hp to 1 for player).
        if (skill.getLethalChance2() > 0 && calcLethalRate(attacker, target, skill.getLethalChance2(), skill.getMagicLevel())) {
            if (target instanceof Npc) {
                target.reduceCurrentHp(target.getStatus().getHp() - 1, attacker, skill);
            } else if (target instanceof Player targetPlayer) {
                targetPlayer.getStatus().setHp(1, false);
                targetPlayer.getStatus().setCp(1);
                targetPlayer.sendPacket(SystemMessageId.LETHAL_STRIKE);
            }
            attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
        }
        // First lethal effect (hp/2 for npc, cp to 1 for player).
        else if (skill.getLethalChance1() > 0 && calcLethalRate(attacker, target, skill.getLethalChance1(), skill.getMagicLevel())) {
            if (target instanceof Npc) {
                target.reduceCurrentHp(target.getStatus().getHp() / 2, attacker, skill);
            } else if (target instanceof Player targetPlayer) {
                targetPlayer.getStatus().setCp(1);
                targetPlayer.sendPacket(SystemMessageId.LETHAL_STRIKE);
            }
            attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
        }
    }

    /**
     * @param attacker : The {@link Creature} launching the blow.
     * @param target : The {@link Creature} victim of the blow.
     * @param skill : The {@link L2Skill} to test.
     * @param sDef : The {@link ShieldDefense} of the target.
     * @param ss : True if ss are activated, false otherwise.
     * @return The calculated blow damage.
     */
    public static double calcBlowDamage(Creature attacker, Creature target, L2Skill skill, ShieldDefense sDef, boolean ss) {
        double defence = target.getStatus().getPDef(attacker);
        switch (sDef) {
            case SUCCESS:
                defence += target.getStatus().getShldDef();
                break;

            case PERFECT:
                return 1.;
        }

        final boolean isPvP = attacker instanceof Playable && target instanceof Playable;

        double attackPower = attacker.getStatus().getPAtk(target);
        double skillPower = skill.getPower();
        final double addCritPower = attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6;

        if (ss) {
            attackPower *= 2.;

            if (skill.getSSBoost() > 0) {
                skillPower *= skill.getSSBoost();
            }
        }

        final double critDamMul = attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill);
        final double rndMul = Rnd.get(95, 105) / 100.;
        final double critDamPosMul = ((attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE_POS, 1, target, skill) - 1) / 2 + 1); // Divided by 2 for blow types.
        final double posMul = getPosMul(attacker, target, true); // Divided by 2 for blow types.
        final double pvpMul = (isPvP) ? attacker.getStatus().calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null) : 1.;

        final double critVuln = target.getStatus().calcStat(Stats.CRIT_VULN, 1, target, skill);
        final double daggerVuln = target.getStatus().calcStat(Stats.DAGGER_WPN_VULN, 1, target, null);

        final double damage = ((attackPower + skillPower) * critDamMul * rndMul * critDamPosMul * posMul * pvpMul * critVuln * daggerVuln + addCritPower) * ((isPvP) ? 70. : 77.) / defence;

        if (Config.DEVELOPER) {
            StringUtil.printSection("Blow damage");
            log.info("ss:{}, shield:{}, isPvp:{}, defence:{}", ss, sDef, isPvP, defence);
            log.info("Basic powers: attack: {}, skill: {}, addCrit: {}", attackPower, skillPower, addCritPower);
            log.info("Multipliers: critDam: {}, rnd: {}, critPos: {}, pos: {}, pvp: {}", critDamMul, rndMul, critDamPosMul, posMul, pvpMul);
            log.info("Vulnerabilities: criticalVuln: {}, daggerVuln: {}", critVuln, daggerVuln);
            log.info("Final blow damage: {}", damage);
        }
        return Math.max(1, damage);
    }

    /**
     * @param attacker : The {@link Creature} launching the physical attack.
     * @param target : The {@link Creature} victim of the physical attack.
     * @param sDef : The {@link ShieldDefense} of the target.
     * @param crit : True if the attack was a critical success, false otherwise.
     * @param ss : True if ss are activated, false otherwise.
     * @return The calculated damage of a physical attack.
     */
    public static double calcPhysicalAttackDamage(Creature attacker, Creature target, ShieldDefense sDef, boolean crit, boolean parried, boolean ss) {
        // If the attacker can't attack, return.
        final Player attackerPlayer = attacker.getActingPlayer();
        if (attackerPlayer != null && !attackerPlayer.getAccessLevel().canGiveDamage()) {
            return 0.;
        }

        double defence = target.getStatus().getPDef(attacker);
        double attackPower = attacker.getStatus().getPAtk(target);
        switch (sDef) {
            case SUCCESS:
                attackPower = Math.max(attackPower - target.getStatus().getShldDef(), attackPower * 0.5);
                break;

            case PERFECT:
                return 1.;
        }

        final boolean isPvP = attacker instanceof Playable && target instanceof Playable;

        double addCritPower = 0.;

        final double pvpMul = (isPvP) ? attacker.getStatus().calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null) : 1.;
        final double posMul = getPosMul(attacker, target, crit);
        final double elemMul = calcElementalPhysicalAttackModifier(attacker, target);
        final double rndMul = attacker.getRandomDamageMultiplier();

        double raceMul = 1.;
        double weaponMul = 1.;
        double critDamMul = 1.;
        double critDamPosMul = 1.;

        double critVuln = 1.;

        // Critic multiplier.
        if (crit) {
            critDamMul = attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE, 1, target, null);
            critDamPosMul = attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE_POS, 1, target, null);

            critVuln = target.getStatus().calcStat(Stats.CRIT_VULN, 1, target, null);

            addCritPower = attacker.getStatus().calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, null) * 77. / defence;
        }

        // Weapon multiplier.
        final Weapon weapon = attacker.getActiveWeaponItem();
        if (weapon != null) {
            final Stats stat = weapon.getItemType().getResStat();
            if (stat != null) {
                weaponMul = target.getStatus().calcStat(stat, 1, target, null);
            }
        }

        // Race multiplier.
        if (target instanceof Npc) {
            final NpcRace race = ((Npc) target).getTemplate().getRace();
            if (race.getAtkStat() != null && race.getResStat() != null) {
                raceMul = 1 + ((attacker.getStatus().calcStat(race.getAtkStat(), 1, target, null) - target.getStatus().calcStat(race.getResStat(), 1, target, null)) / 100);
            }
        }

        // End calculation.
        double damage = 0;
        if (crit) {
            damage = ((attackPower * 2. * critDamMul * critDamPosMul * critVuln * posMul * rndMul * raceMul * pvpMul * elemMul * weaponMul) + addCritPower) * 77. / defence;
        } else {
            damage = (attackPower * posMul * rndMul * raceMul * pvpMul * elemMul * weaponMul) * 77. / defence;
        }

        // If using ss, the damages are multiplied by 2.
        if (ss) {
            damage *= 2.;
        }

        // parry reduce all damage by 50%
        if (parried) {
            damage /= 2;
        }

        if (Config.DEVELOPER) {
            StringUtil.printSection("Physical attack damage");
            log.info("crit:{}, ss:{}, shield:{}, isPvp:{}, defence:{}", crit, ss, sDef, isPvP, defence);
            log.info("Basic powers: attack: {}, addCrit: {}", attackPower, addCritPower);
            log.info("Multipliers: critDam: {}, critPos: {}, pos: {}, rnd: {}, race: {}, pvp: {}, elem: {}, weapon: {}, parry: {}", critDamMul, critDamPosMul, posMul, rndMul, raceMul, pvpMul, elemMul, weaponMul, parried);
            log.info("Vulnerabilities: criticalVuln: {}", critVuln);
            log.info("Final damage: {}", damage);
        }

        if (damage < 0) {
            damage = 0;
        } else if (damage < 1) {
            damage = 1;
        }

        return damage;
    }

    /**
     * @param attacker : The {@link Creature} launching the {@link L2Skill}.
     * @param target : The {@link Creature} victim of the {@link L2Skill}.
     * @param skill : The {@link L2Skill} to test.
     * @param sDef : The {@link ShieldDefense} of the target.
     * @param crit : True if the attack was a critical success, false otherwise.
     * @param ss : True if ss are activated, false otherwise.
     * @return The calculated damage of a physical {@link L2Skill} attack.
     */
    public static double calcPhysicalSkillDamage(Creature attacker, Creature target, L2Skill skill, ShieldDefense sDef, boolean crit, boolean parried, boolean ss) {
        // If the attacker can't attack, return.
        final Player attackerPlayer = attacker.getActingPlayer();
        if (attackerPlayer != null && !attackerPlayer.getAccessLevel().canGiveDamage()) {
            return 0.;
        }

        double attackPower = attacker.getStatus().getPAtk(target);
        double defence = target.getStatus().getPDef(attacker);
        switch (sDef) {
            case SUCCESS:
                attackPower = Math.max(attackPower - target.getStatus().getShldDef(), attackPower * 0.5);
                break;

            case PERFECT:
                return 1.;
        }

        final boolean isPvP = attacker instanceof Playable && target instanceof Playable;


        double skillPower = skill.getPower(attacker);

        final double pvpMul = (isPvP) ? attacker.getStatus().calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null) : 1.;
        final double elemMul = calcElementalSkillModifier(attacker, target, skill);

        double ssMul = 1.;
        double rndMul = 1.;
        double raceMul = 1.;
        double weaponMul = 1.;

        if (ss) {
            ssMul = 2.04;

            if (skill.getSSBoost() > 0) {
                skillPower *= skill.getSSBoost();
            }
        }

        if (parried) {
            skillPower /= 2;
        }

        // Weapon multiplier.
        final Weapon weapon = attacker.getActiveWeaponItem();
        if (weapon != null) {
            final Stats stat = weapon.getItemType().getResStat();
            if (stat != null) {
                weaponMul = target.getStatus().calcStat(stat, 1, target, null);
            }
        }

        // Weapon random damage ; invalid for CHARGEDAM skills.
        if (skill.getSkillType() != SkillType.CHARGEDAM) {
            rndMul = attacker.getRandomDamageMultiplier();
        }

        // Race multiplier.
        if (target instanceof Npc) {
            final NpcRace race = ((Npc) target).getTemplate().getRace();
            if (race.getAtkStat() != null && race.getResStat() != null) {
                raceMul = 1 + ((attacker.getStatus().calcStat(race.getAtkStat(), 1, target, null) - target.getStatus().calcStat(race.getResStat(), 1, target, null)) / 100);
            }
        }

        // End calculation.
        double damage = ((attackPower + skillPower) * ssMul * rndMul * raceMul * pvpMul * elemMul * weaponMul) * 77. / defence;
        if (crit) {
            damage *= 2.;
        }

        if (Config.DEVELOPER) {
            StringUtil.printSection("Physical skill damage");
            log.info("crit:{}, ss:{}, shield:{}, isPvp:{}, defence:{}", crit, ss, sDef, isPvP, defence);
            log.info("Basic powers: attack: {}, skill: {}", attackPower, skillPower);
            log.info("Multipliers: ss: {}, rnd: {}, race: {}, pvp: {}, elem: {}, weapon: {}, parried: {}", ssMul, rndMul, raceMul, pvpMul, elemMul, weaponMul, parried);
            log.info("Final damage: {}", damage);
        }

        if (damage < 0) {
            damage = 0;
        } else if (damage < 1) {
            damage = 1;
        }

        return damage;
    }

    public static double calcMagicDam(Creature attacker, Creature target, L2Skill skill, ShieldDefense sDef, boolean ss, boolean bss, boolean mcrit) {
        // If the attacker can't attack, return.
        final Player attackerPlayer = attacker.getActingPlayer();
        if (attackerPlayer != null && !attackerPlayer.getAccessLevel().canGiveDamage()) {
            return 0.;
        }

        double mAtk = attacker.getStatus().getMAtk(target, skill);
        double mDef = target.getStatus().getMDef(attacker, skill);
        switch (sDef) {
            case SUCCESS:
                mAtk = Math.max(mAtk - target.getStatus().getShldDef(), mAtk * 0.5);
                break;

            case PERFECT:
                return 1.;
        }

        if (bss) {
            mAtk *= 4;
        } else if (ss) {
            mAtk *= 2;
        }

        double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker);

        // Failure calculation
        if (Config.MAGIC_FAILURES && !calcMagicSuccess(attacker, target, skill)) {
            if (attacker instanceof Player) {
                if (calcMagicSuccess(attacker, target, skill) && (target.getStatus().getLevel() - attacker.getStatus().getLevel()) <= 9) {
                    if (skill.getSkillType() == SkillType.DRAIN) {
                        attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
                    } else {
                        attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
                    }

                    damage /= 2;
                } else {
                    attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
                    damage = 1;
                }
            }

            if (target instanceof Player) {
                if (skill.getSkillType() == SkillType.DRAIN) {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(attacker));
                } else {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(attacker));
                }
            }
        } else if (mcrit) {
            damage *= 4;
        }

        // Pvp bonuses for dmg
        if (attacker instanceof Playable && target instanceof Playable) {
            if (skill.isMagic()) {
                damage *= attacker.getStatus().calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
            } else {
                damage *= attacker.getStatus().calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
            }
        }

        damage *= calcElementalSkillModifier(attacker, target, skill);

        return damage;
    }

    public static double calcMagicDam(Cubic attacker, Creature target, L2Skill skill, boolean mcrit, ShieldDefense sDef) {
        double mDef = target.getStatus().getMDef(attacker.getOwner(), skill);
        switch (sDef) {
            case SUCCESS:
                mDef += target.getStatus().getShldDef();
                break;

            case PERFECT:
                return 1.;
        }

        double damage = 91 / mDef * skill.getPower();
        Player owner = attacker.getOwner();

        // Failure calculation
        if (Config.MAGIC_FAILURES && !calcMagicSuccess(owner, target, skill)) {
            if (calcMagicSuccess(owner, target, skill) && (target.getStatus().getLevel() - skill.getMagicLevel()) <= 9) {
                if (skill.getSkillType() == SkillType.DRAIN) {
                    owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
                } else {
                    owner.sendPacket(SystemMessageId.ATTACK_FAILED);
                }

                damage /= 2;
            } else {
                owner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
                damage = 1;
            }

            if (target instanceof Player) {
                if (skill.getSkillType() == SkillType.DRAIN) {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(owner));
                } else {
                    target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(owner));
                }
            }
        } else if (mcrit) {
            damage *= 4;
        }

        damage *= calcElementalSkillModifier(owner, target, skill);

        return damage;
    }

    /**
     * @param actor
     * @param target
     * @param skill
     * @return true in case of critical hit
     */
    public static boolean calcCrit(Creature actor, Creature target, L2Skill skill) {
        return calcCrit(actor.getStatus().getCriticalHit(target, skill));
    }

    public static boolean calcCrit(double rate) {
        return rate > Rnd.get(1000);
    }

    public static boolean calcMCrit(Creature actor, Creature target, L2Skill skill) {
        final int mRate = actor.getStatus().getMCriticalHit(target, skill);

        if (Config.DEVELOPER) {
            log.info("Current mCritRate: {} / 1000.", mRate);
        }

        return mRate > Rnd.get(1000);
    }

    /**
     * Check if casting process is canceled due to hit.
     *
     * @param target The target to make checks on.
     * @param dmg The amount of dealt damages.
     */
    public static void calcCastBreak(Creature target, double dmg) {
        // Don't go further for invul characters or raid bosses.
        if (target.isRaidRelated() || target.isInvul()) {
            return;
        }

        // Break automatically the skill cast if under attack.
        if (target.getFusionSkill() != null) {
            target.getCast().interrupt();
            return;
        }

        // Don't go further for ppl casting a physical skill.
        if (target.getCast().getCurrentSkill() != null && !target.getCast().getCurrentSkill().isMagic()) {
            return;
        }

        // Calculate all modifiers for ATTACK_CANCEL ; chance to break is higher with higher dmg, and is affected by target MEN.
        double rate = target.getStatus().calcStat(Stats.ATTACK_CANCEL, 15 + Math.sqrt(13 * dmg) - (MEN_BONUS[target.getStatus().getMEN()] * 100 - 100), null, null);

        if (Config.DEVELOPER) {
            StringUtil.printSection("Cast break rate");
            log.info("Final cast break rate: {}%.", rate);
        }

        if (MathUtil.limit((int) rate, 1, 99) > Rnd.get(100)) {
            target.getCast().interrupt();
        }
    }

    /**
     * @param attacker : The {@link Creature} who attacks.
     * @return The delay, in ms, before the next attack.
     */
    public static int calculateTimeBetweenAttacks(Creature attacker) {
        return Math.max(100, 500000 / attacker.getStatus().getPAtkSpd());
    }

    /**
     * Calculate delay (in milliseconds) for skills cast.
     *
     * @param attacker
     * @param skill used to know if skill is magic or no.
     * @param skillTime
     * @return delay in ms.
     */
    public static int calcAtkSpd(Creature attacker, L2Skill skill, double skillTime) {
        if (skill.isMagic()) {
            return (int) (skillTime * 333 / attacker.getStatus().getMAtkSpd());
        }

        return (int) (skillTime * 333 / attacker.getStatus().getPAtkSpd());
    }

    /**
     * Calculate the hit/miss chance.
     *
     * @param attacker : The {@link Creature} attacker to test.
     * @param target : The {@link Creature} target to test.
     * @return True if the hit missed or false if it evaded.
     */
    public static boolean calcHitMiss(Creature attacker, Creature target, L2Skill skill) {

        if (skill != null) {
            if (!skill.isDamage() && !skill.isProjectile()) {
                return false; // always hit
            }
        }

        int diff = attacker.getStatus().getAccuracy() - target.getStatus().getEvasionRate(attacker);

        // Get high or low Z bonus.
        final int diffZ = attacker.getZ() - target.getZ();
        if (diffZ > 50) {
            diff += 3;
        } else if (diffZ < -50) {
            diff -= 3;
        }

        // Get weather bonus.
        if (DayNightTaskManager.getInstance().is(DayCycle.NIGHT)) {
            diff -= 10;
        }

        // Get position bonus.
        if (attacker.isBehind(target)) {
            diff += 10;
        } else if (!attacker.isInFrontOf(target)) {
            diff += 5;
        }

        int chance = MathUtil.limit((100 + (2 * (diff))) * 10, skill != null ? 300 : 50, 980);
        if (Config.DEVELOPER) {
            log.info("calcHitMiss diff: {}, rate: {}%.", diff, chance / 10);
        }

        return !Rnd.calcChance(chance, 1000);
    }

    /**
     * @param attacker : The {@link Creature} used as attacker.
     * @param target : The {@link Creature} used as target ; his shield is checked.
     * @param skill : The {@link L2Skill} used by the attacker, if any.
     * @param isCrit : If true, we apply a x3 block rate.
     * @return A {@link ShieldDefense} based on calculated shield rate.
     */
    public static ShieldDefense calcShldUse(Creature attacker, Creature target, L2Skill skill, boolean isCrit) {
        // "Ignore shield" skills types bypass the shield use.
        if (skill != null && skill.ignoreShield()) {
            return ShieldDefense.FAILED;
        }

        // No shield is actually equipped, return as FAILED.
        final Item item = target.getSecondaryWeaponItem();
        if (!(item instanceof Armor)) {
            return ShieldDefense.FAILED;
        }

        // The calculated shield rate is 0, return as FAILED.
        final double baseRate = target.getStatus().calcStat(Stats.SHIELD_RATE, 0, attacker, null);
        if (baseRate == 0.0) {
            return ShieldDefense.FAILED;
        }

        // The degree doesn't match, return as FAILED.
        final int degreeSide = (int) target.getStatus().calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null);
        if (degreeSide < 360 && !target.isFacing(attacker, degreeSide)) {
            return ShieldDefense.FAILED;
        }

        final double dexMul = DEX_BONUS[target.getStatus().getDEX()];

        double shieldRate = baseRate * dexMul;

        // Shield block rate is multiplied by 3 if the attacker uses a bow.
        final boolean isBow = attacker.getAttackType() == WeaponType.BOW;
        if (isBow) {
            shieldRate *= 3;
        }

        // Shield block rate is multiplied by 3 during critical hits.
        if (isCrit) {
            shieldRate *= 3;
        }

        final int chance = Rnd.get(100);

        // Calculate the ShieldDefense.
        ShieldDefense sDef;
        if (chance < target.getStatus().calcStat(Stats.PERFECT_BLOCK, Config.PERFECT_SHIELD_BLOCK_RATE, attacker, null)) {
            sDef = ShieldDefense.PERFECT;
        } else if (chance < shieldRate) {
            sDef = ShieldDefense.SUCCESS;
        } else {
            sDef = ShieldDefense.FAILED;
        }

        // Send message to Player target.
        if (target instanceof Player) {
            switch (sDef) {
                case SUCCESS:
                    ((Player) target).sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
                    break;

                case PERFECT:
                    ((Player) target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
                    break;
            }
        }

        if (Config.DEVELOPER) {
            StringUtil.printSection("Shield use");
            log.info("Basic values: baseRate: {}, degreeSide: {}", baseRate, degreeSide);
            log.info("Multipliers: dex: {}, isBow: {}, isCrit: {}", dexMul, (isBow) ? "3.0" : "0.", (isCrit) ? "3.0" : "0.");
            log.info("ShieldDefense: {}, rate: {} / 100, chance: {}", sDef, shieldRate, chance);
        }

        return sDef;
    }

    public static boolean calcParryRate(Creature attacker, Creature target, L2Skill skill) {
        // paryy can be used only if has a skill of same type
        // todo: add this skill to all monsters in world with isParryWeapon type
        if (!target.hasSkill(462)) {
            return false;
        }

        // only special attack type (weapon type) can have parry
        WeaponType attackType = target.getAttackType();
        if (!attackType.isParryWeapon()) {
            return false;
        }

        // we can parry only in front
        if (!attacker.isInFrontOf(target)) {
            return false;
        }

        // DEX increases parry rate chance as multiplier
        double dexBonus = DEX_BONUS[target.getStatus().getDEX()];
        double parryRate = target.getStatus().calcStat(Stats.PARRY_RATE, 0, target, skill) * dexBonus;

        // night is reduce parry rate
        if (DayNightTaskManager.getInstance().is(DayCycle.NIGHT)) {
            // but only if race is not dark_elf
            if (!(target instanceof Player player) || player.getRace() != ClassRace.DARK_ELF) {
                parryRate /= 2;
            }
        }

        // with dual-weapon parry rate is increased by x2
        if (attackType == WeaponType.DUAL || attackType == WeaponType.DUALFIST) {
            parryRate *= 2;
        }

        // parry of arrows is multiplied of x2
        if (attacker.getAttackType() == WeaponType.BOW) {
            parryRate *= 2;
        }

        return Rnd.calcChance(parryRate * 10, 1000);
    }

    public static boolean calcMagicAffected(Creature actor, Creature target, L2Skill skill) {
        double defence = 0;

        if (skill.isActive() && skill.isOffensive()) {
            defence = target.getStatus().getMDef(actor, skill);
        }

        double attack = 2 * actor.getStatus().getMAtk(target, skill);
        double d = (attack - defence) / (attack + defence);

        d += 0.5 * Rnd.nextGaussian();
        return d > 0;
    }

    private static double calcSkillStatModifier(SkillType type, Creature target, boolean isMagic) {
        double multiplier = 1;

        switch (type) {
            case STUN:
                multiplier = 2 - SQRT_CON_BONUS[target.getStatus().getCON()];
                break;

            case SLEEP:
            case DEBUFF:
            case WEAKNESS:
            case ERASE:
            case ROOT:
            case MUTE:
            case FEAR:
            case BETRAY:
            case CONFUSION:
            case AGGREDUCE_CHAR:
            case PARALYZE:
                if (isMagic) {
                    multiplier = 2 - SQRT_MEN_BONUS[target.getStatus().getMEN()];
                }
                break;
        }

        return Math.max(0, multiplier);
    }

    public static double getSTRBonus(Creature activeChar) {
        return STR_BONUS[activeChar.getStatus().getSTR()];
    }

    private static double getLevelModifier(Creature attacker, Creature target, L2Skill skill) {
        if (skill.getLevelDepend() == 0) {
            return 1;
        }

        int delta = (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getStatus().getLevel()) - target.getStatus().getLevel();
        return 1 + ((delta < 0 ? 0.01 : 0.005) * delta);
    }

    private static double getMatkModifier(Creature attacker, Creature target, L2Skill skill, boolean bss) {
        double mAtkModifier = 1;

        if (skill.isMagic()) {
            final double mAtk = attacker.getStatus().getMAtk(target, skill);
            double val = mAtk;
            if (bss) {
                val = mAtk * 4.0;
            }

            mAtkModifier = (Math.sqrt(val) / target.getStatus().getMDef(attacker, skill)) * 11.0;
        }
        return mAtkModifier;
    }

    public static boolean calcEffectSuccess(Creature attacker, Creature target, EffectTemplate effect, L2Skill skill, boolean bss) {
        SkillType type = effect.getEffectType();
        attacker.increasePseudo(effect, effect.getEffectPower());
        double chance = attacker.getPseudoChance(effect);

        if (type == null || skill.ignoreResists()) {
            return Rnd.calcChance(chance, 100);
        }

        if (type.equals(SkillType.CANCEL)) {
            return true;
        }

        final double statModifier = calcSkillStatModifier(type, target, skill.isMagic());
        final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
        final double lvlModifier = getLevelModifier(attacker, target, skill);
        chance = Math.max(1, Math.min((chance * statModifier * mAtkModifier * lvlModifier), 99));

        if (Config.DEVELOPER) {
            log.info("calcEffectSuccess(): name:{} eff.type:{} power:{} statMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), type, chance, String.format("%1.2f", statModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", chance));
        }

        if (attacker.isGM()) {
            String report = String.format("SkillSuccess[%s].chance: statMod=%s, mAtkMod=%s, lvlMod=%s, chance=%s", skill.getName(),
                String.format("%1.2f", statModifier),
                String.format("%1.2f", mAtkModifier),
                String.format("%1.2f", lvlModifier),
                String.format("%1.2f", chance));
            attacker.sendMessage(report);
        }

        boolean success = Rnd.calcChance(chance, 100);
        if (success) {
            attacker.increasePseudo(effect, 100. / 10);
            log.info("Effect {} next chance: {}%", skill.getName(), attacker.getPseudoChance(effect));
        } else {
            attacker.clearPseudo(skill);
            log.info("Pseudo cleared for {}", effect);
        }

        return success;
    }

    public static boolean calcSkillSuccess(Creature attacker, Creature target, L2Skill skill, ShieldDefense sDef, boolean bss) {
        if (sDef == ShieldDefense.PERFECT) {
            return false;
        }

        SkillType type = skill.getSkillType();
        attacker.increasePseudo(skill, skill.getEffectPower());
        double chance = attacker.getPseudoChance(skill);

        if (skill.ignoreResists()) {
            return Rnd.calcChance(chance, 100);
        }

        final double statModifier = calcSkillStatModifier(type, target, skill.isMagic());
        final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
        final double lvlModifier = getLevelModifier(attacker, target, skill);
        chance = Math.max(1, Math.min((chance * statModifier * mAtkModifier * lvlModifier), 100));

        if (Config.DEVELOPER) {
            log.info("calcSkillSuccess(): name:{} type:{} power:{} statMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), skill.getSkillType(), chance, String.format("%1.2f", statModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", chance));
        }

        if (attacker.isGM()) {
            String report = String.format("SkillSuccess[%s].chance: statMod=%s, mAtkMod=%s, lvlMod=%s, chance=%s", skill.getName(),
                String.format("%1.2f", statModifier),
                String.format("%1.2f", mAtkModifier),
                String.format("%1.2f", lvlModifier),
                String.format("%1.2f", chance));
            attacker.sendMessage(report);
        }

        boolean success = Rnd.calcChance(chance, 100);
        if (!success) {
            attacker.increasePseudo(skill, 100. / 10);
            log.info("Skill {} next chance: {}%", skill.getName(), attacker.getPseudoChance(skill));
        } else {
            attacker.clearPseudo(skill);
            log.info("Pseudo cleared for {}", skill);
        }

        return success;
    }

    public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, L2Skill skill, ShieldDefense sDef, boolean bss) {
        // if target reflect this skill then the effect will fail
        if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED) {
            return false;
        }

        if (sDef == ShieldDefense.PERFECT) {
            return false;
        }

        SkillType type = skill.getSkillType();
        double chance = skill.getEffectPower();

        if (skill.ignoreResists()) {
            return Rnd.calcChance(chance, 100);
        }

        double mAtkModifier = 1;

        // Add Matk/Mdef Bonus
        if (skill.isMagic()) {
            final double mAtk = attacker.getMAtk();
            double val = mAtk;
            if (bss) {
                val = mAtk * 4.0;
            }

            mAtkModifier = (Math.sqrt(val) / target.getStatus().getMDef(null, null)) * 11.0;
        }

        double statModifier = calcSkillStatModifier(type, target, skill.isMagic());
        double lvlModifier = getLevelModifier(attacker.getOwner(), target, skill);
        chance = Math.max(1, Math.min((chance * statModifier * mAtkModifier * lvlModifier), 99));

        if (Config.DEVELOPER) {
            log.info("calcCubicSkillSuccess(): name:{} type:{} power:{} statMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), skill.getSkillType(), chance, String.format("%1.2f", statModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", chance));
        }

        if (attacker.getOwner().isGM()) {
            String report = String.format("SkillSuccess[%s].chance: statMod=%s, lvlMod=%s, chance=%s", skill.getName(),
                String.format("%1.2f", statModifier),
                String.format("%1.2f", lvlModifier),
                String.format("%1.2f", chance));
            attacker.getOwner().sendMessage(report);
        }

        return Rnd.calcChance(chance, 100);
    }

    public static boolean calcMagicSuccess(Creature attacker, Creature target, L2Skill skill) {
        int lvlDifference = target.getStatus().getLevel() - ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getStatus().getLevel()) + skill.getLevelDepend());
        double rate = 100;

        if (lvlDifference > 0) {
            rate = (Math.pow(1.166, lvlDifference)) * 100;
        }

        if (attacker instanceof Player && ((Player) attacker).getWeaponGradePenalty()) {
            rate += 6000;
        }

        if (Config.DEVELOPER) {
            log.info("calcMagicSuccess(): name:{} lvlDiff:{} fail:{}%.", skill.getName(), lvlDifference, String.format("%1.2f", rate / 100));
        }

        rate = Math.min(rate, 9900);

        return (Rnd.get(10000) > rate);
    }

    public static double calcManaDam(Creature attacker, Creature target, L2Skill skill, boolean ss, boolean bss) {
        double mAtk = attacker.getStatus().getMAtk(target, skill) * (bss ? 4 : ss ? 2 : 1);
        double mDef = target.getStatus().getMDef(attacker, skill);
        double mp = target.getStatus().getMaxMp();
        return (Math.sqrt(mAtk) * skill.getPower(attacker) * (mp / 97.0)) / mDef;
    }

    public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster) {
        if (baseRestorePercent == 0 || baseRestorePercent == 100) {
            return baseRestorePercent;
        }

        double restorePercent = baseRestorePercent * WIT_BONUS[caster.getStatus().getWIT()];
        if (restorePercent - baseRestorePercent > 20.0) {
            restorePercent += 20.0;
        }

        restorePercent = Math.max(restorePercent, baseRestorePercent);
        restorePercent = Math.min(restorePercent, 90.0);

        return restorePercent;
    }

    public static boolean calcPhysicalSkillEvasion(Creature target, L2Skill skill) {
        if (skill.isMagic()) {
            return false;
        }

        return Rnd.calcChance(target.getStatus().calcStat(Stats.P_SKILL_EVASION, 0, null, skill), 100);
    }

    public static boolean calcSkillMastery(Creature actor, L2Skill sk) {
        // Pointless check for Creature other than players, as initial value will stay 0.
        if (!(actor instanceof Player)) {
            return false;
        }

        if (sk.getSkillType() == SkillType.FISHING) {
            return false;
        }

        double val = actor.getStatus().calcStat(Stats.SKILL_MASTERY, 0, null, null);

        if (((Player) actor).isMageClass()) {
            val *= INT_BONUS[actor.getStatus().getINT()];
        } else {
            val *= STR_BONUS[actor.getStatus().getSTR()];
        }

        return Rnd.get(100) < val;
    }

    /**
     * Calculate the elemental modifier, based on one {@link Creature} attacker and one {@link Creature} target.
     *
     * @param attacker : The {@link Creature} used to retrieve elemental attacks.
     * @param target : The {@link Creature} used to retrieve elemental protections.
     * @return A multiplier based on attacker {@link ElementType} attack traits, or 1.
     */
    private static double calcElementalPhysicalAttackModifier(Creature attacker, Creature target) {
        double elemMod = 1.;
        for (ElementType element : ElementType.VALUES) {
            if (element == ElementType.NONE) {
                continue;
            }

            final int traitAmount = attacker.getStatus().getAttackElementValue(element);
            if (traitAmount > 0) {
                elemMod *= target.getStatus().getDefenseElementValue(element);
            }
        }
        return elemMod;
    }

    /**
     * Calculate the elemental modifier, based on one {@link Creature} attacker, one {@link Creature} target and a
     * {@link L2Skill}.
     *
     * @param attacker : The {@link Creature} used to retrieve elemental attacks.
     * @param target : The {@link Creature} used to retrieve elemental protections.
     * @param skill : If different of null, it will be considered as a {@link L2Skill} resist check.
     * @return A multiplier based on the {@link ElementType} of the {@link L2Skill}, or 1.
     */
    private static double calcElementalSkillModifier(Creature attacker, Creature target, L2Skill skill) {
        if (skill != null) {
            final ElementType element = skill.getElement();
            if (element != ElementType.NONE) {
                return 1 + (((attacker.getStatus().getAttackElementValue(element) / 10.0) / 100.0) - (1 - target.getStatus().getDefenseElementValue(element)));
            }
        }
        return 1.;
    }

    /**
     * Calculate skill reflection according to these three possibilities:
     * <ul>
     * <li>Reflect failed</li>
     * <li>Normal reflect (just effects).</li>
     * <li>Vengeance reflect (100% damage reflected but damage is also dealt to actor).</li>
     * </ul>
     *
     * @param target : The skill's target.
     * @param skill : The skill to test.
     * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or SKILL_REFLECT_VENGEANCE
     */
    public static byte calcSkillReflect(Creature target, L2Skill skill) {
        // Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
        if (skill.ignoreResists() || !skill.canBeReflected()) {
            return SKILL_REFLECT_FAILED;
        }

        // Only magic and melee skills can be reflected.
        if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE)) {
            return SKILL_REFLECT_FAILED;
        }

        byte reflect = SKILL_REFLECT_FAILED;

        // Check for non-reflected skilltypes, need additional retail check.
        switch (skill.getSkillType()) {
            case BUFF:
            case REFLECT:
            case HEAL_PERCENT:
            case MANAHEAL_PERCENT:
            case HOT:
            case MPHOT:
            case AGGDEBUFF:
            case CONT:
                return SKILL_REFLECT_FAILED;

            case PDAM:
            case BLOW:
            case MDAM:
            case DEATHLINK:
            case CHARGEDAM:
                final double vengeanceChance = target.getStatus().calcStat((skill.isMagic()) ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
                if (vengeanceChance > Rnd.get(100)) {
                    reflect |= SKILL_REFLECT_VENGEANCE;
                }
                break;
        }

        final double reflectChance = target.getStatus().calcStat((skill.isMagic()) ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
        if (Rnd.get(100) < reflectChance) {
            reflect |= SKILL_REFLECT_SUCCEED;
        }

        return reflect;
    }

    public static boolean calcEffectReflect(Creature target, L2Skill skill) {
        // Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
        if (skill.ignoreResists() || !skill.canBeReflected()) {
            return false;
        }

        // Only magic and melee skills can be reflected.
        if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE)) {
            return false;
        }

        // Check for non-reflected skilltypes, need additional retail check.
        switch (skill.getSkillType()) {
            case BUFF:
            case REFLECT:
            case HEAL_PERCENT:
            case MANAHEAL_PERCENT:
            case HOT:
            case MPHOT:
            case AGGDEBUFF:
            case CONT:
                return false;
        }

        final double reflectChance = target.getStatus().calcStat((skill.isMagic()) ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
        return Rnd.calcChance(reflectChance, 100);
    }

    public static boolean calcSkillVengeance(Creature target, L2Skill skill) {
        // Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
        if (skill.ignoreResists() || !skill.canBeReflected()) {
            return false;
        }

        // Only magic and melee skills can be reflected.
        if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE)) {
            return false;
        }

        // Check for non-reflected skilltypes, need additional retail check.
        return switch (skill.getSkillType()) {
            case PDAM, BLOW, MDAM, DEATHLINK, CHARGEDAM -> {
                final double vengeanceChance = target.getStatus().calcStat((skill.isMagic())
                    ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE
                    : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
                yield Rnd.calcChance(vengeanceChance, 100);
            }
            default -> false;
        };

    }

    /**
     * @param actor : The character affected.
     * @param fallHeight : The height the NPC fallen.
     * @return the damage, based on max HPs and falling height.
     */
    public static double calcFallDam(Creature actor, int fallHeight) {
        if (!Config.ENABLE_FALLING_DAMAGE || fallHeight < 0) {
            return 0;
        }

        return actor.getStatus().calcStat(Stats.FALL, fallHeight * actor.getStatus().getMaxHp() / 1000., null, null);
    }

    /**
     * Calculates karma lost upon death.
     *
     * @param level : The level of the PKer.
     * @param exp : The amount of xp earned.
     * @return The amount of karma player has lost.
     */
    public static int calculateKarmaLost(int level, long exp) {
        return (int) (exp / PlayerLevelData.getInstance().getPlayerLevel(level).getKarmaModifier() / 15);
    }

    /**
     * Calculates karma gain upon player kill.
     *
     * @param pkCount : The current number of PK kills.
     * @param isSummon : Does the victim is a summon or no (lesser karma gain if true).
     * @return karma points that will be added to the player.
     */
    public static int calculateKarmaGain(int pkCount, boolean isSummon) {
        int result = 14400;
        if (pkCount < 100) {
            result = (int) (((((pkCount - 1) * 0.5) + 1) * 60) * 4);
        } else if (pkCount < 180) {
            result = (int) (((((pkCount + 1) * 0.125) + 37.5) * 60) * 4);
        }

        if (isSummon) {
            result = ((pkCount & 3) + result) >> 2;
        }

        return result;
    }

    /**
     * @param attacker : The {@link Creature} launching the attack.
     * @param target : The {@link Creature} victim of the attack.
     * @param crit : If true, we divide by 2 the multiplier.
     * @return The position multiplier for physical hit / skills and blows.
     */
    private static double getPosMul(Creature attacker, Creature target, boolean crit) {
        if (attacker.isBehind(target)) {
            return (crit) ? 1.1 : 1.2;
        }

        if (!attacker.isInFrontOf(target)) {
            return (crit) ? 1.025 : 1.05;
        }

        return 1.;
    }

    public static double calcNegateSkillPower(L2Skill skill, Creature caster, Creature target) {
        return Math.pow(skill.getLevel(), caster.getStatus().getLevel() - target.getStatus().getLevel() - 5);
    }

    public static int calcResistPeriod(Creature target, AbstractEffect effect, int period) {
        if (effect.getSkill().ignoreResists()) {
            return 1;
        }

        int skillId = effect.getSkill().getId();

        if (skillId > 2277 && skillId < 2286
            && (target instanceof Servitor || (target instanceof Player && target.getSummon() != null))) {
            period /= 2;
        }

        double multiplier = 1.0;

        multiplier = switch (effect.getEffectSkillType()) {
            case STUN -> target.getStatus().calcStat(Stats.STUN_VULN, multiplier, target, null);
            case PARALYZE -> target.getStatus().calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
            case ROOT -> target.getStatus().calcStat(Stats.ROOT_VULN, multiplier, target, null);
            case SLEEP -> target.getStatus().calcStat(Stats.SLEEP_VULN, multiplier, target, null);
            case MUTE, BETRAY, AGGDEBUFF, AGGREDUCE_CHAR, ERASE ->
                target.getStatus().calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
            case DEBUFF, WEAKNESS -> target.getStatus().calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
            default -> multiplier;
        };

        return Math.max((int) (period * multiplier), 1);
    }

    public static int calcResistCounter(Creature target, AbstractEffect effect, int counter) {
        double multiplier = 1;

        if (effect.getSkill().ignoreResists()) {
            return (int) multiplier;
        }

        multiplier = switch (effect.getEffectSkillType()) {
            case BLEED -> target.getStatus().calcStat(Stats.BLEED_VULN, multiplier, target, null);
            case POISON -> target.getStatus().calcStat(Stats.POISON_VULN, multiplier, target, null);
            case FEAR, CONFUSION -> target.getStatus().calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
            default -> multiplier;
        };

        return Math.max((int) (counter * multiplier), 1);
    }

    public static int calcProjectileFlyTime(Creature attacker, Creature target, int baseFlyTime) {
        return (int) ((attacker.distance2D(target.getPosition()) * 0.3333333333) + (baseFlyTime * 0.7777777777) + 200);
    }

    public static int calcWeaponFractureValue(Player attacker, Creature target, L2Skill skill, Default.Context context, ItemInstance weapon) {
        ItemInstance activeWeaponInstance = attacker.getActiveWeaponInstance();

        // null & FIST weapon nul reduce durability, drop calculation
        if (activeWeaponInstance == null || attacker.getAttackType() == WeaponType.FIST) {
            return 0;
        }

        WeaponType weaponType = attacker.getAttackType();

        // calc for weapon if use magic
        if (skill != null && skill.isMagic()) {
            // magic reduce durability by MP consuming value & weapon grade
            int gradeModifier = weapon.getItem().getCrystalType().getId() + 1;
            double mpConsume = skill.getMpConsume() + skill.getMpInitialConsume();
            return (int) Math.max(Math.sqrt(mpConsume) + gradeModifier, 1);
        } else {
            // bow always reduce durability by 4
            if (weaponType == WeaponType.BOW) {
                return 4;
            }

            double damage = context.getValue();
            double augmentedMod = activeWeaponInstance.isAugmented() ? 0.85 : 1.;
            double blockMod = context.getBlock() == ShieldDefense.PERFECT ? Rnd.get(3, 10) : context.getBlock() == ShieldDefense.SUCCESS ? 2 : 1;
            double armorMod = 1;

            // if target is player and he's equipped by armor
            if (target instanceof Player player) {
                // against armor modifier for target=player
                ItemInstance armor = player.getInventory().getRandomEquippedItem(0);
                if (armor != null) {
                    armorMod = ((ArmorType) armor.getItemType()).getDamageToWeaponDurability();
                }
            } else {
                // against monster which level is lower than attacker level
                armorMod = target.getStatus().getLevel() - attacker.getStatus().getLevel();
                if (armorMod < -9) {
                    return 1;
                }
            }

            double value = Math.max(damage * augmentedMod * blockMod * armorMod, 1);
            double reduce = Math.sqrt(value);
            return (int) reduce;
        }
    }

    public static int calcArmorFractureValue(L2Skill skill, Default.Context context, Armor armor) {
        // if doesnt have equipped armor
        if (armor == null) {
            return 0;
        }

        double damage = context.getValue();
        // when attack was blocked by shield
        double blockMod = blockFractureValue(context.getBlock());
        // armor durability absorb
        double armorMod = armor.getItemType().getDurabilityAbsorb();

        if (skill != null && skill.isMagic()) {
            if (skill.isDamage() && armor.isAccessory()) {
                armorMod = 1.;
            } else if (skill.isDebuff() && context.isSuccess()) {
                // target armor is fracture from debuffs
                damage = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : 80;
            }
        }

        double reduce = Math.sqrt(damage * blockMod * armorMod);
        return (int) Math.max(reduce, 1);
    }

    private static double blockFractureValue(ShieldDefense block) {
        if (!block.isSuccess()) {
            return 1.;
        }

        if (block == ShieldDefense.PERFECT) {
            return 0.01;
        }

        return ArmorType.SHIELD.getDurabilityAbsorb();
    }
}