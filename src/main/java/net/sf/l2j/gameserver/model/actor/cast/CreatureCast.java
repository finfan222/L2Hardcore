package net.sf.l2j.gameserver.model.actor.cast;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceled;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.utils.Multicast;
import net.sf.l2j.gameserver.skills.utils.Recoil;

import java.util.concurrent.ScheduledFuture;

/**
 * This class groups all cast data related to a {@link Creature}.
 *
 * @param <T> : The {@link Creature} used as actor.
 */
@Slf4j
public class CreatureCast<T extends Creature> {

    protected final T _caster;

    protected long _castInterruptTime;

    protected Creature[] _targets;
    protected Creature _target;
    protected L2Skill _skill;
    protected int _hitTime;
    protected int _coolTime;

    protected ScheduledFuture<?> _castTask;

    private boolean _isCastingNow;

    public CreatureCast(T actor) {
        _caster = actor;
    }

    public final boolean canAbortCast() {
        return _castInterruptTime > System.currentTimeMillis();
    }

    public final boolean isCastingNow() {
        return _isCastingNow;
    }

    public final L2Skill getCurrentSkill() {
        return _skill;
    }

    public void doFusionCast(L2Skill skill, Creature target) {
        // Non-Player Creatures cannot use FUSION or SIGNETS
    }

    public void doInstantCast(L2Skill itemSkill, ItemInstance item) {
        // Non-Playable Creatures cannot use potions or energy stones
    }

    public void doToggleCast(L2Skill skill, Creature target) {
        // Non-Player Creatures cannot use TOGGLES
    }

    /**
     * Manage the casting task and display the casting bar and animation on client.
     *
     * @param skill : The {@link L2Skill} to cast.
     * @param target : The {@link Creature} effected target.
     * @param itemInstance : The potential {@link ItemInstance} used to cast.
     */
    public void doCast(L2Skill skill, Creature target, ItemInstance itemInstance) {
        int hitTime = skill.getHitTime();
        int coolTime = skill.getCoolTime();
        if (!skill.isStaticHitTime()) {
            hitTime = Formulas.calcAtkSpd(_caster, skill, hitTime);
            if (coolTime > 0) {
                coolTime = Formulas.calcAtkSpd(_caster, skill, coolTime);
            }

            if (skill.isMagic() && (_caster.isChargedShot(ShotType.SPIRITSHOT) || _caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT))) {
                hitTime = (int) (0.70 * hitTime);
                coolTime = (int) (0.70 * coolTime);
            }

            if (skill.getHitTime() >= 500 && hitTime < 500) {
                hitTime = 500;
            }
        }

        int reuseDelay = skill.getReuseDelay();
        if (!skill.isAbility() && !skill.isStaticReuse()) {
            reuseDelay = (int) _caster.getStatus().calcStat(skill.isMagic() ? Stats.MAGIC_REUSE_RATE : Stats.P_REUSE, reuseDelay, null, null);
            reuseDelay = (int) (reuseDelay * (333.0 / (skill.isMagic() ? _caster.getStatus().getMAtkSpd() : _caster.getStatus().getPAtkSpd())));
        }

        final boolean skillMastery = Formulas.calcSkillMastery(_caster, skill);
        if (skillMastery) {
            if (_caster.getActingPlayer() != null) {
                _caster.getActingPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
            }
        } else {
            if (reuseDelay > 30000) {
                _caster.addTimeStamp(skill, reuseDelay);
            }

            if (reuseDelay > 10) {
                _caster.disableSkill(skill, reuseDelay);
            }
        }

        final int initMpConsume = _caster.getStatus().getMpInitialConsume(skill);
        if (initMpConsume > 0) {
            _caster.getStatus().reduceMp(initMpConsume);
        }

        if (target != _caster) {
            _caster.getPosition().setHeadingTo(target);
        }

        _caster.broadcastPacket(new MagicSkillUse(_caster, target, skill.getId(), skill.getLevel(), hitTime, reuseDelay, false));

        if (itemInstance == null) {
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));
        }

        final long castInterruptTime = System.currentTimeMillis() + hitTime - 200;

        setCastTask(skill, target, hitTime, coolTime, castInterruptTime);

        if (_hitTime > 410) {
            if (_caster instanceof Player) {
                _caster.sendPacket(new SetupGauge(GaugeColor.BLUE, _hitTime));
            }
        } else {
            _hitTime = 0;
        }

        _castTask = ThreadPool.schedule(this::onMagicLaunch, hitTime > 410 ? hitTime - 400 : 0);
    }

    /**
     * Manage the launching task and display the animation on client.
     */
    private final void onMagicLaunch() {
        // Content was cleaned meantime, simply return doing nothing.
        if (!isCastingNow()) {
            return;
        }

        // No checks for range, LoS, PEACE if the target is the caster.
        if (_target != _caster) {
            int escapeRange = 0;
            if (_skill.getEffectRange() > 0) {
                escapeRange = _skill.getEffectRange();
            } else if (_skill.getCastRange() <= 0 && _skill.getSkillRadius() > 80) {
                escapeRange = _skill.getSkillRadius();
            }

            // If the target disappears, stop the cast.
            if (_caster.getAI().isTargetLost(_target, _skill)) {
                stop();
                return;
            }

            // If the target is out of range, stop the cast.
            if (escapeRange > 0 && !MathUtil.checkIfInRange(escapeRange, _caster, _target, true)) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));

                stop();
                return;
            }

            // If the target is out of view, stop the cast.
            if (_skill.getSkillRadius() > 0 && !GeoEngine.getInstance().canSeeTarget(_caster, _target)) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));

                stop();
                return;
            }

            // If the target reached a PEACE zone, stop the cast.
            if (_skill.isOffensive() && _caster instanceof Playable && _target instanceof Playable) {
                if (_caster.isInsideZone(ZoneId.PEACE)) {
                    _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_ATK_PEACEZONE));

                    stop();
                    return;
                }

                if (_target.isInsideZone(ZoneId.PEACE)) {
                    _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));

                    stop();
                    return;
                }
            }
        }

        _targets = _skill.getTargetList(_caster, _target);

        _caster.broadcastPacket(new MagicSkillLaunched(_caster, _skill, _targets));

        int flyTime = 400;
        if (_skill.isProjectile()) {
            flyTime = Formulas.calcProjectileFlyTime(_caster, _target, flyTime);
        } else {
            flyTime = _hitTime == 0 ? 0 : flyTime;
        }

        _castTask = ThreadPool.schedule(this::onMagicHitTimer, flyTime);
    }

    /**
     * Manage effects application, after cast animation occured. Verify if conditions are still met.
     */
    private void onMagicHitTimer() {
        // Content was cleaned meantime, simply return doing nothing.
        if (!isCastingNow()) {
            return;
        }

        final double mpConsume = _caster.getStatus().getMpConsume(_skill);
        if (mpConsume > 0) {
            if (mpConsume > _caster.getStatus().getMp()) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
                stop();
                return;
            }

            _caster.getStatus().reduceMp(mpConsume);
        }

        final double hpConsume = _skill.getHpConsume();
        if (hpConsume > 0) {
            if (hpConsume > _caster.getStatus().getHp()) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
                stop();
                return;
            }

            _caster.getStatus().reduceHp(hpConsume, _caster, true);
        }

        if (_caster instanceof Player && _skill.getNumCharges() > 0) {
            if (_skill.getMaxCharges() > 0) {
                ((Player) _caster).increaseCharges(_skill.getNumCharges(), _skill.getMaxCharges());
            } else {
                ((Player) _caster).decreaseCharges(_skill.getNumCharges());
            }
        }

        for (final Creature target : _targets) {
            if (target instanceof Summon && _caster instanceof Player) {
                ((Summon) target).updateAndBroadcastStatus(1);
            }
        }

        callSkill(_skill, _targets);

        _castTask = ThreadPool.schedule(this::onMagicFinalizer, (_hitTime == 0 || _coolTime == 0) ? 0 : _coolTime);
    }

    /**
     * Manage the end of a cast launch.
     */
    protected final void onMagicFinalizer() {
        // Content was cleaned meantime, simply return doing nothing.
        if (!isCastingNow()) {
            return;
        }

        _caster.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());

        if (_skill.isOffensive() && _targets.length != 0) {
            _caster.getAI().startAttackStance();
        }

        final Creature target = _targets.length > 0 ? _targets[0] : _target;
        _caster.notifyQuestEventSkillFinished(_skill, target);

        clearCastTask();
        _caster.getAI().notifyEvent(AiEventType.FINISHED_CASTING, null, null);
    }

    /**
     * Check cast conditions BEFORE MOVEMENT.
     *
     * @param target : The {@link Creature} used as parameter.
     * @param skill : The {@link L2Skill} used as parameter.
     * @return True if casting is possible, false otherwise.
     */
    public boolean canAttemptCast(Creature target, L2Skill skill) {
        if (_caster.isSkillDisabled(skill)) {
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill));
            return false;
        }

        return true;
    }

    /**
     * Check cast conditions AFTER MOVEMENT.
     *
     * @param target : The {@link Creature} used as parameter.
     * @param skill : The {@link L2Skill} used as parameter.
     * @param isCtrlPressed : If True, we use specific CTRL rules.
     * @param itemObjectId : If different than 0, an object has been used.
     * @return True if casting is possible, false otherwise.
     */
    public boolean canDoCast(Creature target, L2Skill skill, boolean isCtrlPressed, int itemObjectId) {
        final int initialMpConsume = _caster.getStatus().getMpInitialConsume(skill);
        final int mpConsume = _caster.getStatus().getMpConsume(skill);

        if ((initialMpConsume > 0 || mpConsume > 0) && (int) _caster.getStatus().getMp() < initialMpConsume + mpConsume) {
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
            return false;
        }

        if (skill.getHpConsume() > 0 && (int) _caster.getStatus().getHp() <= skill.getHpConsume()) {
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
            return false;
        }

        if ((skill.isMagic() && _caster.isMuted()) || (!skill.isMagic() && _caster.isPhysicalMuted())) {
            return false;
        }

        if (skill.getCastRange() > 0 && !GeoEngine.getInstance().canSeeTarget(_caster, target)) {
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
            return false;
        }

        if (!skill.getWeaponDependancy(_caster)) {
            return false;
        }

        return true;
    }

    /**
     * Abort the current cast, no matter actual cast step.
     */
    public final void stop() {
        if (_caster.getFusionSkill() != null) {
            _caster.getFusionSkill().onCastAbort();
        }

        final AbstractEffect effect = _caster.getFirstEffect(EffectType.SIGNET_GROUND);
        if (effect != null) {
            effect.exit();
        }

        if (_caster.isAllSkillsDisabled()) {
            _caster.enableAllSkills();
        }

        if (isCastingNow()) {
            _caster.broadcastPacket(new MagicSkillCanceled(_caster.getObjectId()));
        }

        if (_castTask != null) {
            _castTask.cancel(false);
            _castTask = null;
        }

        clearCastTask();

        _caster.getAI().tryToActive();
        _caster.getAI().clientActionFailed();
    }

    /**
     * Interrupt the current cast, if it is still breakable.
     */
    public void interrupt() {
        if (canAbortCast()) {
            stop();
            _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTING_INTERRUPTED));
        }
    }

    /**
     * Launch the magic skill and calculate its effects on each target contained in the targets array.
     *
     * @param skill : The {@link L2Skill} to use.
     * @param targets : The array of {@link Creature} targets.
     */
    public void callSkill(L2Skill skill, Creature[] targets) {
        // Raid Curses system.
        if (_caster instanceof Playable && _caster.testCursesOnSkillSee(skill, targets)) {
            return;
        }

        for (final Creature target : targets) {
            if (_caster instanceof Playable && target instanceof Monster monster && skill.isOverhit()) {
                monster.getOverhitState().setActivated(true);
            }

            switch (skill.getSkillType()) {
                case COMMON_CRAFT:
                case DWARVEN_CRAFT:
                    break;

                default:
                    final Weapon activeWeaponItem = _caster.getActiveWeaponItem();
                    if (activeWeaponItem != null && !target.isDead()) {
                        activeWeaponItem.castSkillOnMagic(_caster, target, skill);
                    }

                    if (_caster.getChanceSkills() != null) {
                        _caster.getChanceSkills().onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
                    }

                    if (target.getChanceSkills() != null) {
                        target.getChanceSkills().onSkillHit(_caster, true, skill.isMagic(), skill.isOffensive());
                    }
            }
        }

        skill.useSkill(_caster, targets);

        if (skill.isRecoiled()) {
            Recoil.start(_caster, targets[0], skill, skill.getRecoilChance(), skill.getRecoilCount(), skill.isRecoilPlayerPriority(), skill.getRecoilRadius());
        } else if(skill.isMulticasted()) {
            Multicast.start(_caster, targets[0], skill, skill.getMulticastCount(), skill.getMulticastChance(), skill.getMulticastHitTime());
        }

        final Player player = _caster.getActingPlayer();
        if (player != null) {
            for (final Creature target : targets) {
                if (skill.isOffensive()) {
                    if (player.getSummon() != target) {
                        player.updatePvPStatus(target);
                    }
                } else {
                    if (target instanceof Playable) {
                        final Player targetPlayer = target.getActingPlayer();
                        if (!(targetPlayer.equals(_caster) || targetPlayer.equals(player)) && (targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0)) {
                            player.updatePvPStatus();
                        }
                    } else if (target instanceof Attackable && !((Attackable) target).isGuard()) {
                        switch (skill.getSkillType()) {
                            case SUMMON:
                            case BEAST_FEED:
                            case UNLOCK:
                            case UNLOCK_SPECIAL:
                            case DELUXE_KEY_UNLOCK:
                                break;

                            default:
                                player.updatePvPStatus();
                        }
                    }
                }

                switch (skill.getTargetType()) {
                    case CORPSE_MOB:
                    case AREA_CORPSE_MOB:
                        if (target instanceof Npc && target.isDead()) {
                            ((Npc) target).endDecayTask();
                        }
                        break;
                    default:
                        break;
                }
            }

            // Notify NPCs in a 1000 range of a skill use.
            for (Npc npc : _caster.getKnownTypeInRadius(Npc.class, 1000)) {
                for (Quest quest : npc.getTemplate().getEventQuests(ScriptEventType.ON_SKILL_SEE)) {
                    quest.notifySkillSee(npc, player, skill, targets, _caster instanceof Summon);
                }
            }
        }

        if (skill.isOffensive()) {
            switch (skill.getSkillType()) {
                case AGGREDUCE:
                case AGGREMOVE:
                case AGGREDUCE_CHAR:
                    break;

                default:
                    for (final Creature target : targets) {
                        if (target != null && target.hasAI()) {
                            target.getAI().notifyEvent(AiEventType.ATTACKED, _caster, null);
                        }
                    }
                    break;
            }
        }
    }

    protected void clearCastTask() {
        _isCastingNow = false;
    }

    protected void setCastTask(L2Skill skill, Creature target, int hitTime, int coolTime, long castInterruptTime) {
        _skill = skill;
        _target = target;
        _hitTime = hitTime;
        _coolTime = coolTime;
        _castInterruptTime = castInterruptTime;
        _isCastingNow = true;
    }
}