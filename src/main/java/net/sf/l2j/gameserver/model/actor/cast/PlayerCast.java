package net.sf.l2j.gameserver.model.actor.cast;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.handlers.SiegeFlag;
import net.sf.l2j.gameserver.skills.handlers.StriderSiegeAssault;
import net.sf.l2j.gameserver.skills.handlers.SummonFriend;
import net.sf.l2j.gameserver.skills.handlers.SummonServitor;
import net.sf.l2j.gameserver.skills.handlers.TakeCastle;

/**
 * This class groups all cast data related to a {@link Player}.
 */
public class PlayerCast extends PlayableCast<Player> {
    private final Location _signetLocation = new Location(Location.DUMMY_LOC);

    public PlayerCast(Player actor) {
        super(actor);
    }

    @Override
    public void doFusionCast(L2Skill skill, Creature target) {
        final int reuseDelay = skill.getReuseDelay();

        final boolean skillMastery = Formulas.calcSkillMastery(_caster, skill);
        if (skillMastery) {
            _caster.sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
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

        _targets = new Creature[]
            {
                target
            };

        final int hitTime = skill.getHitTime();
        final int coolTime = skill.getCoolTime();
        final long castInterruptTime = System.currentTimeMillis() + hitTime - 200;

        setCastTask(skill, target, hitTime, coolTime, castInterruptTime);

        if (skill.getSkillType() == SkillType.FUSION) {
            _caster.startFusionSkill(target, skill);
        } else {
            callSkill(skill, _targets);
        }

        _caster.broadcastPacket(new MagicSkillUse(_caster, target, skill.getId(), skill.getLevel(), hitTime, reuseDelay, false));
        _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));
        _caster.sendPacket(new SetupGauge(GaugeColor.BLUE, _hitTime));

        _castTask = ThreadPool.schedule(this::onMagicEffectHitTimer, hitTime > 410 ? hitTime - 400 : 0);
    }

    @Override
    public void doInstantCast(L2Skill skill, ItemInstance item) {
        if (!item.isHerb() && !_caster.destroyItem("Consume", item.getObjectId(), (skill.getItemConsumeId() == 0 && skill.getItemConsume() > 0) ? skill.getItemConsume() : 1, null, false)) {
            _caster.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            return;
        }

        int reuseDelay = skill.getReuseDelay();
        if (reuseDelay > 10) {
            _caster.disableSkill(skill, reuseDelay);
        }

        _caster.broadcastPacket(new MagicSkillUse(_caster, _caster, skill.getId(), skill.getLevel(), 0, 0));

        _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_S1).addSkillName(skill));

        if (skill.getNumCharges() > 0) {
            if (skill.getMaxCharges() > 0) {
                _caster.increaseCharges(skill.getNumCharges(), skill.getMaxCharges());
            } else {
                _caster.decreaseCharges(skill.getNumCharges());
            }
        }

        callSkill(skill, new Creature[]
            {
                _caster
            });
    }

    @Override
    public void doToggleCast(L2Skill skill, Creature target) {
        setCastTask(skill, target, 0, 0, 0);

        _caster.broadcastPacket(new MagicSkillUse(_caster, _caster, _skill.getId(), _skill.getLevel(), 0, 0));

        _targets = new Creature[]
            {
                _target
            };

        // If the toggle is already active, we don't need to do anything else besides stopping it.
        final AbstractEffect effect = _caster.getFirstEffect(_skill.getId());
        if (effect != null) {
            effect.exit();
        } else {
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


            _skill.useSkill(_caster, _targets);
        }

        _castTask = ThreadPool.schedule(this::onMagicFinalizer, 0);
    }

    @Override
    public void doCast(L2Skill skill, Creature target, ItemInstance itemInstance) {
        super.doCast(skill, target, itemInstance);

        if (skill.getItemConsumeId() > 0) {
            _caster.destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true);
        }

        _caster.clearRecentFakeDeath();
    }

    @Override
    public boolean canAttemptCast(Creature target, L2Skill skill) {
        if (!super.canAttemptCast(target, skill)) {
            return false;
        }

        if (_caster.isWearingFormalWear()) {
            _caster.sendPacket(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR);
            return false;
        }

        final SkillType skillType = skill.getSkillType();
        if (_caster.isFishing() && (skillType != SkillType.PUMPING && skillType != SkillType.REELING && skillType != SkillType.FISHING)) {
            _caster.sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
            return false;
        }

        if (_caster.isInObserverMode()) {
            _caster.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
            return false;
        }

        if (_caster.isSitting() && !_caster.isFakeDeath()) {
            _caster.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
            return false;
        }

        if (_caster.isFakeDeath() && skill.getId() != 60) {
            _caster.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
            return false;
        }

        if (skill.getTargetType() == SkillTargetType.GROUND && _signetLocation.equals(Location.DUMMY_LOC)) {
            return false;
        }

        if (_caster.isInDuel()) {
            final Player targetPlayer = target.getActingPlayer();
            if (targetPlayer != null && targetPlayer.getDuelId() != _caster.getDuelId()) {
                _caster.sendPacket(SystemMessageId.INVALID_TARGET);
                return false;
            }
        }

        if (skill.isSiegeSummonSkill()) {
            final Siege siege = CastleManager.getInstance().getActiveSiege(_caster);
            if (siege == null || !siege.checkSide(_caster.getClan(), SiegeSide.ATTACKER)) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
                return false;
            }

            if (_caster.isInsideZone(ZoneId.CASTLE)) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_CALL_PET_FROM_THIS_LOCATION));
                return false;
            }

            if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) == CabalType.DAWN && SevenSignsManager.getInstance().getPlayerCabal(_caster.getObjectId()) == CabalType.DUSK) {
                _caster.sendPacket(SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canDoCast(Creature target, L2Skill skill, boolean isCtrlPressed, int itemObjectId) {
        if (!super.canDoCast(target, skill, isCtrlPressed, itemObjectId)) {
            return false;
        }

        switch (skill.getSkillType()) {
            case SUMMON:
                if (!((SummonServitor) skill).isCubic() && (_caster.getSummon() != null || _caster.isMounted())) {
                    _caster.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
                    return false;
                }
                break;

            case RESURRECT:
                final Siege siege = CastleManager.getInstance().getActiveSiege(_caster);
                if (siege != null) {
                    if (_caster.getClan() == null) {
                        _caster.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
                        return false;
                    }

                    final SiegeSide side = siege.getSide(_caster.getClan());
                    if (side == SiegeSide.DEFENDER || side == SiegeSide.OWNER) {
                        if (siege.getControlTowerCount() == 0) {
                            _caster.sendPacket(SystemMessageId.TOWER_DESTROYED_NO_RESURRECTION);
                            return false;
                        }
                    } else if (side == SiegeSide.ATTACKER) {
                        if (_caster.getClan().getFlag() == null) {
                            _caster.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
                            return false;
                        }
                    } else {
                        _caster.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
                        return false;
                    }
                }
                break;

            case SPOIL:
            case DRAIN_SOUL:
                if (!(target instanceof Monster)) {
                    _caster.sendPacket(SystemMessageId.INVALID_TARGET);
                    return false;
                }
                break;

            case SWEEP:
                if (skill.getTargetType() != SkillTargetType.AREA_CORPSE_MOB) {
                    final int spoilerId = ((Monster) target).getSpoilState().getSpoilerId();
                    if (spoilerId == 0) {
                        _caster.sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
                        return false;
                    }

                    if (!_caster.isLooterOrInLooterParty(spoilerId)) {
                        _caster.sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);
                        return false;
                    }
                }
                break;

            case TAKE_CASTLE:
                if (TakeCastle.check(_caster, target, skill, true) == null) {
                    return false;
                }

                break;

            case SIEGE_FLAG:
                if (!SiegeFlag.check(_caster, false)) {
                    return false;
                }

                break;

            case STRIDER_SIEGE_ASSAULT:
                if (!StriderSiegeAssault.check(_caster, target, skill)) {
                    return false;
                }

                break;

            case SUMMON_FRIEND:
                if (!(SummonFriend.checkSummoner(_caster) && SummonFriend.checkSummoned(_caster, target))) {
                    return false;
                }

                break;
        }
        return true;
    }

    /**
     * Used by {@link #doFusionCast(L2Skill, Creature)}
     */
    private final void onMagicEffectHitTimer() {
        // Content was cleaned meantime, simply return doing nothing.
        if (!isCastingNow()) {
            return;
        }

        _targets = _skill.getTargetList(_caster, _target);

        if (_caster.getFusionSkill() != null) {
            _caster.getFusionSkill().onCastAbort();

            clearCastTask();
            return;
        }

        _caster.broadcastPacket(new MagicSkillLaunched(_caster, _skill, _targets));

        _caster.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());

        final double mpConsume = _caster.getStatus().getMpConsume(_skill);
        if (mpConsume > 0) {
            if (mpConsume > _caster.getStatus().getMp()) {
                _caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
                stop();
                return;
            }

            _caster.getStatus().reduceMp(mpConsume);
        }

        _castTask = ThreadPool.schedule(this::onMagicEffectFinalizer, 400);
    }

    /**
     * Used by {@link #doFusionCast(L2Skill, Creature)}
     */
    public void onMagicEffectFinalizer() {
        _caster.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());

        if (_skill.isOffensive() && _targets.length != 0) {
            _caster.getAI().startAttackStance();
        }

        clearCastTask();

        _caster.getAI().notifyEvent(AiEventType.FINISHED_CASTING, null, null);
    }

    public Location getSignetLocation() {
        return _signetLocation;
    }
}