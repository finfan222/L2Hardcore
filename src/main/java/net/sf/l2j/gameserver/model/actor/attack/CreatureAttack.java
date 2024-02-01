package net.sf.l2j.gameserver.model.actor.attack;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.events.OnAttacked;
import net.sf.l2j.gameserver.events.OnAttacking;
import net.sf.l2j.gameserver.events.OnHit;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.creature.ChanceSkillList;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Attack;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

/**
 * This class groups all attack data related to a {@link Creature}.
 *
 * @param <T> : The {@link Creature} used as actor.
 */
@Slf4j
@Getter
public class CreatureAttack<T extends Creature> {

    protected final T attacker;

    private boolean isAttackingNow;
    private boolean isBowCoolingDown;
    private HitHolder[] hits;
    private WeaponType type;
    private int coolTime;
    private ScheduledFuture<?> task;

    public CreatureAttack(T attacker) {
        this.attacker = attacker;
    }

    public boolean isAttackingNow() {
        return isAttackingNow;
    }

    public boolean isBowCoolingDown() {
        return isBowCoolingDown;
    }

    /**
     * @param target The target to check
     * @return True if the attacker doesn't have isAttackingDisabled
     */
    public boolean canDoAttack(Creature target) {
        if (attacker.isAttackingDisabled()) {
            return false;
        }

        if (!target.isAttackableBy(attacker) || !attacker.knows(target)) {
            return false;
        }

        if (!GeoEngine.getInstance().canSeeTarget(attacker, target)) {
            attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
            return false;
        }

        return true;
    }

    /**
     * Manage hit process (called by Hit Task).<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a Player)</li>
     * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are Player</li>
     * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
     * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
     * </ul>
     */
    private void onHitTimer() {
        // Content was cleaned meantime, simply return doing nothing.
        if (!isAttackingNow()) {
            return;
        }

        // Something happens to the target between the attacker attacking and the actual damage being dealt.
        // There is no PEACE zone check here. If the attack starts outside and in the meantime the mainTarget walks into a PEACE zone, it gets hit.
        final Creature mainTarget = hits[0].target;
        if (mainTarget.isDead() || !attacker.knows(mainTarget)) {
            stop();
            return;
        }

        final Player player = attacker.getActingPlayer();

        // Player can't flag if attacking his Summon, and vice-versa.
        if (player != null && player.getSummon() != mainTarget && !(player.getSummon() == attacker && mainTarget == player)) {
            player.updatePvPStatus(mainTarget);
        }

        attacker.rechargeShots(true, false);

        // Test curses. Prevents messing up drop calculation.
        if (attacker instanceof Playable && mainTarget.isRaidRelated() && attacker.testCursesOnAttack((Npc) mainTarget)) {
            stop();
            return;
        }

        switch (type) {
            case DUAL:
                doHit(hits[0]);

                task = ThreadPool.schedule(() ->
                {
                    // Content was cleaned meantime, simply return doing nothing.
                    if (!isAttackingNow()) {
                        return;
                    }

                    doHit(hits[1]);

                    task = ThreadPool.schedule(this::onFinishedAttack, coolTime);
                }, coolTime);
                break;

            case POLE:
                for (HitHolder hitHolder : hits) {
                    doHit(hitHolder);
                }

                task = ThreadPool.schedule(this::onFinishedAttack, coolTime);
                break;

            case BOW:
                doHit(hits[0]);

                isBowCoolingDown = true;

                task = ThreadPool.schedule(() ->
                {
                    isBowCoolingDown = false;
                    attacker.getAI().notifyEvent(AiEventType.BOW_ATTACK_REUSED, null, null);

                }, coolTime);

                onFinishedAttackBow();
                break;

            default:
                doHit(hits[0]);

                task = ThreadPool.schedule(this::onFinishedAttack, coolTime);
                break;
        }
    }

    private void onFinishedAttackBow() {
        clearAttackTask(false);

        attacker.getAI().notifyEvent(AiEventType.FINISHED_ATTACK_BOW, null, null);
    }

    private void onFinishedAttack() {
        clearAttackTask(false);

        attacker.getAI().notifyEvent(AiEventType.FINISHED_ATTACK, null, null);
    }

    private void doHit(HitHolder hitHolder) {
        final Creature target = hitHolder.target;
        if (hitHolder.isMissed) {
            if (target.hasAI()) {
                target.getAI().notifyEvent(AiEventType.EVADED, attacker, null);
            }

            if (target.getChanceSkills() != null) {
                target.getChanceSkills().onEvadedHit(attacker);
            }

            if (target instanceof Player) {
                target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(attacker));
            }
        }

        attacker.sendDamageMessage(target, hitHolder.damage, false, hitHolder.isCritical, hitHolder.isMissed);

        if (!hitHolder.isMissed && hitHolder.damage > 0) {
            attacker.getAI().startAttackStance();

            if (target.hasAI()) {
                target.getAI().notifyEvent(AiEventType.ATTACKED, attacker, null);
            }

            int reflectedDamage = 0;

            // Reflect damage system - do not reflect if weapon is a bow or target is invulnerable
            if (type != WeaponType.BOW && !target.isInvul()) {
                // quick fix for no drop from raid if boss attack high-level char with damage reflection
                if (!target.isRaidRelated() || attacker.getActingPlayer() == null || attacker.getActingPlayer().getStatus().getLevel() <= target.getStatus().getLevel() + 8) {
                    // Calculate reflection damage to reduce HP of attacker if necessary
                    final double reflectPercent = target.getStatus().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
                    if (reflectPercent > 0) {
                        reflectedDamage = (int) (reflectPercent / 100. * hitHolder.damage);

                        if (reflectedDamage > target.getStatus().getMaxHp()) {
                            reflectedDamage = target.getStatus().getMaxHp();
                        }
                    }
                }
            }

            // Reduce target HPs
            target.reduceCurrentHp(hitHolder.damage, attacker, null);

            // Reduce attacker HPs in case of a reflect.
            if (reflectedDamage > 0) {
                attacker.reduceCurrentHp(reflectedDamage, target, true, false, null);
            }

            // Calculate the absorbed HP percentage. Do not absorb if weapon is a bow.
            if (type != WeaponType.BOW) {
                final double absorbPercent = attacker.getStatus().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
                if (absorbPercent > 0) {
                    attacker.getStatus().addHp(absorbPercent / 100. * hitHolder.damage);
                }
            }

            // Manage cast break of the target (calculating rate, sending message...)
            Formulas.calcCastBreak(target, hitHolder.damage);

            // Maybe launch chance skills on us
            final ChanceSkillList chanceSkills = attacker.getChanceSkills();
            if (chanceSkills != null) {
                chanceSkills.onHit(target, false, hitHolder.isCritical);

                // Reflect triggers onHit
                if (reflectedDamage > 0) {
                    chanceSkills.onHit(target, true, false);
                }
            }

            // Maybe launch chance skills on target
            if (target.getChanceSkills() != null) {
                target.getChanceSkills().onHit(attacker, true, hitHolder.isCritical);
            }

            // Launch weapon Special ability effect if available
            if (hitHolder.isCritical) {
                final Weapon activeWeapon = attacker.getActiveWeaponItem();
                if (activeWeapon != null) {
                    activeWeapon.castSkillOnCrit(attacker, target);
                }
            }
        }

        attacker.getEventListener().notify(new OnHit(attacker, target, hitHolder));
        target.getEventListener().notify(new OnHit(attacker, target, hitHolder));
    }

    /**
     * Launch a physical attack against a {@link Creature}.
     *
     * @param target : The {@link Creature} used as target.
     * @return True if the hit was actually successful, false otherwise.
     */
    public boolean doAttack(Creature target) {
        final int timeAtk = Formulas.calculateTimeBetweenAttacks(attacker);
        final Weapon weaponItem = attacker.getActiveWeaponItem();
        final Attack attack = new Attack(attacker, attacker.isChargedShot(ShotType.SOULSHOT), (weaponItem != null) ? weaponItem.getCrystalType().getId() : 0);

        attacker.getPosition().setHeadingTo(target);

        HitHolder[] hits = switch (attacker.getAttackType()) {
            case BOW -> doAttackHitByBow(attack, target, timeAtk, weaponItem);
            case POLE -> doAttackHitByPole(attack, target, timeAtk / 2);
            case DUAL, DUALFIST -> doAttackHitByDual(attack, target, timeAtk / 2);
            case FIST -> (attacker.getSecondaryWeaponItem() instanceof Armor)
                ? doAttackHitSimple(attack, target, timeAtk / 2)
                : doAttackHitByDual(attack, target, timeAtk / 2);
            default -> doAttackHitSimple(attack, target, timeAtk / 2);
        };

        // Process attack, store result.
        final boolean isHit = attack.processHits(hits);

        // Check if hit isn't missed ; if we didn't miss the hit, discharge the shoulshots, if any.
        if (isHit) {
            attacker.setChargedShot(ShotType.SOULSHOT, false);
        }

        if (attack.hasHits()) {
            attacker.broadcastPacket(attack);
        }

        attacker.getEventListener().notify(new OnAttacking<>(getAttacker(), target, this));
        target.getEventListener().notify(new OnAttacked<>(getAttacker(), target, this));
        return isHit;
    }

    /**
     * Launch a Bow attack.
     *
     * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
     * @param target : The targeted {@link Creature}.
     * @param sAtk : The Attack Speed of the attacker.
     * @param weapon : The {@link Weapon} used to retrieve the reuse delay.
     * @return An array of generated {@link HitHolder}s.
     */
    private HitHolder[] doAttackHitByBow(Attack attack, Creature target, int sAtk, Weapon weapon) {
        attacker.reduceArrowCount();

        final HitHolder[] hits = new HitHolder[]
            {
                getHitHolder(attack, target, false)
            };

        int reuse = weapon.getReuseDelay();
        if (reuse != 0) {
            reuse = (reuse * 345) / attacker.getStatus().getPAtkSpd();
        }

        setAttackTask(hits, WeaponType.BOW, reuse);

        task = ThreadPool.schedule(this::onHitTimer, Formulas.calcProjectileFlyTime(attacker, target, sAtk));

        if (attacker instanceof Player) {
            attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));
            attacker.sendPacket(new SetupGauge(GaugeColor.RED, sAtk + reuse));
        }
        return hits;
    }

    /**
     * Launch a Dual wield attack.
     *
     * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
     * @param target : The targeted {@link Creature}.
     * @param sAtk : The Attack Speed of the attacker.
     * @return An array of generated {@link HitHolder}s.
     */
    private HitHolder[] doAttackHitByDual(Attack attack, Creature target, int sAtk) {
        final HitHolder[] hits = new HitHolder[]
            {
                getHitHolder(attack, target, true),
                getHitHolder(attack, target, true)
            };

        setAttackTask(hits, WeaponType.DUAL, sAtk / 2);

        task = ThreadPool.schedule(this::onHitTimer, sAtk / 2);

        return hits;
    }

    /**
     * Launch a Pole attack.
     *
     * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
     * @param target : The targeted {@link Creature}.
     * @param sAtk : The Attack Speed of the attacker.
     * @return An array of generated {@link HitHolder}s.
     */
    private HitHolder[] doAttackHitByPole(Attack attack, Creature target, int sAtk) {
        final ArrayList<HitHolder> hitHolders = new ArrayList<>();
        hitHolders.add(getHitHolder(attack, target, false));

        final int maxAttackedCount;
        if (attacker.getFirstEffect(EffectType.POLEARM_TARGET_SINGLE) != null) {
            maxAttackedCount = 1;
        } else {
            maxAttackedCount = (int) attacker.getStatus().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null);
        }

        if (maxAttackedCount > 1) {
            final int maxAngleDiff = (int) attacker.getStatus().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
            final boolean isMainTargetPlayable = target instanceof Playable;

            int attackedCount = 1;

            for (Creature knownCreature : attacker.getKnownTypeInRadius(Creature.class, attacker.getStatus().getPhysicalAttackRange())) {
                if (knownCreature == target) {
                    continue;
                }

                if (!attacker.isFacing(knownCreature, maxAngleDiff)) {
                    continue;
                }

                if (!knownCreature.isAttackableBy(attacker)) {
                    continue;
                }

                if (attacker instanceof Playable && knownCreature instanceof Playable && (knownCreature.isInsideZone(ZoneId.PEACE) || !isMainTargetPlayable || !knownCreature.isAttackableWithoutForceBy((Playable) attacker))) {
                    continue;
                }

                attackedCount++;
                if (attackedCount > maxAttackedCount) {
                    break;
                }

                hitHolders.add(getHitHolder(attack, knownCreature, false));
            }
        }

        final HitHolder[] hits = hitHolders.toArray(new HitHolder[]{});

        setAttackTask(hits, WeaponType.POLE, sAtk);

        task = ThreadPool.schedule(this::onHitTimer, sAtk);

        return hits;
    }

    /**
     * Launch a simple attack.
     *
     * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
     * @param target : The targeted {@link Creature}.
     * @param sAtk : The Attack Speed of the attacker.
     * @return An array of generated {@link HitHolder}s.
     */
    private HitHolder[] doAttackHitSimple(Attack attack, Creature target, int sAtk) {
        final HitHolder[] hits = new HitHolder[]
            {
                getHitHolder(attack, target, false)
            };

        setAttackTask(hits, WeaponType.ETC, sAtk);

        task = ThreadPool.schedule(this::onHitTimer, sAtk);

        return hits;
    }

    /**
     * @param attack : The {@link Attack} serverpacket in which {@link HitHolder}s will be added.
     * @param target : The targeted {@link Creature}.
     * @param isSplit : If true, damages will be split in 2. Used for dual wield attacks.
     * @return a new {@link HitHolder} with generated damage, shield resistance, critical and miss informations.
     */
    private HitHolder getHitHolder(Attack attack, Creature target, boolean isSplit) {
        boolean crit = false;
        ShieldDefense shld = ShieldDefense.FAILED;
        int damage = 0;

        final boolean miss = Formulas.calcHitMiss(attacker, target);
        if (!miss) {
            crit = Formulas.calcCrit(attacker, target, null);
            shld = Formulas.calcShldUse(attacker, target, null, crit);
            damage = (int) Formulas.calcPhysicalAttackDamage(attacker, target, shld, crit, attack.soulshot);

            if (isSplit) {
                damage /= 2;
            }
        }

        return new HitHolder(target, damage, crit, miss, shld);
    }

    /**
     * Abort the current attack of the {@link Creature} and send {@link ActionFailed} packet.
     */
    public final void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }

        clearAttackTask(true);

        attacker.getAI().tryToActive();
        attacker.getAI().clientActionFailed();
    }

    /**
     * Abort the current attack and send {@link SystemMessageId#ATTACK_FAILED} to the {@link Creature}.
     */
    public void interrupt() {
        if (isAttackingNow()) {
            stop();
            attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
        }
    }

    private void setAttackTask(HitHolder[] hitHolders, WeaponType weaponType, int afterAttackDelay) {
        isAttackingNow = true;
        isBowCoolingDown = (weaponType == WeaponType.BOW);
        hits = hitHolders;
        type = weaponType;
        this.coolTime = afterAttackDelay;
    }

    private void clearAttackTask(boolean clearBowCooldown) {
        isAttackingNow = false;

        if (clearBowCooldown) {
            isBowCoolingDown = false;
        }
    }

    public static class HitHolder {

        public Creature target;
        public int targetId;
        public int damage;
        public boolean isCritical;
        public boolean isMissed;
        public ShieldDefense block;
        public int flags;

        public HitHolder(Creature target, int damage, boolean isCritical, boolean isMissed, ShieldDefense block) {
            this.target = target;
            this.targetId = target.getObjectId();
            this.damage = damage;
            this.isCritical = isCritical;
            this.isMissed = isMissed;
            this.block = block;
        }

    }
}