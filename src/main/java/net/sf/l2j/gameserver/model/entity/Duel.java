package net.sf.l2j.gameserver.model.entity;

import lombok.Getter;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.data.manager.DuelManager;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.graveyard.DieReason;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelReady;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelStart;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class Duel {
    public enum DuelState {
        NO_DUEL,
        ON_COUNTDOWN,
        DUELLING,
        DEAD,
        WINNER,
        INTERRUPTED
    }

    private static final PlaySound B04_S01 = new PlaySound(1, "B04_S01");

    private enum DuelResult {
        CONTINUE,
        TEAM_1_WIN,
        TEAM_2_WIN,
        TEAM_1_SURRENDER,
        TEAM_2_SURRENDER,
        CANCELED,
        TIMEOUT
    }

    private final int _duelId;
    private final boolean _isPartyDuel;
    private final Calendar _duelEndTime;
    private final Player _playerA;
    private final Player _playerB;
    private final List<PlayerCondition> _playerConditions = new CopyOnWriteArrayList<>();
    @Getter
    private boolean isMortalCombat;

    private int _surrenderRequest;

    protected Future<?> _startTask = null;
    protected Future<?> _checkTask = null;
    protected int _countdown = 5;

    public Duel(Player playerA, Player playerB, boolean isPartyDuel, int duelId) {
        _duelId = duelId;
        _playerA = playerA;
        _playerB = playerB;
        _isPartyDuel = isPartyDuel;

        _duelEndTime = Calendar.getInstance();
        _duelEndTime.add(Calendar.SECOND, 120);

        if (isMortalCombat) {
            _countdown = 60;
        }

        if (_isPartyDuel) {
            _countdown = 35;

            // Inform players that they will be ported shortly.
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
            broadcastToTeam1(sm);
            broadcastToTeam2(sm);

            for (Player partyPlayer : _playerA.getParty().getMembers()) {
                partyPlayer.setInDuel(_duelId);
            }

            for (Player partyPlayer : _playerB.getParty().getMembers()) {
                partyPlayer.setInDuel(_duelId);
            }
        } else {
            // Set states.
            _playerA.setInDuel(_duelId);
            _playerB.setInDuel(_duelId);
        }

        savePlayerConditions();

        // Start task, used for countdowns and startDuel method call. Can be shutdowned if the check task commands it.
        _startTask = ThreadPool.scheduleAtFixedRate(new StartTask(), 1000, 1000);

        // Check task, used to verify if duel is disturbed.
        _checkTask = ThreadPool.scheduleAtFixedRate(new CheckTask(), 1000, 1000);
    }

    public Duel(Player playerA, Player playerB, boolean isPartyDuel, int duelId, boolean isMortalCombat) {
        this(playerA, playerB, isPartyDuel, duelId);
        this.isMortalCombat = isMortalCombat;
    }

    /**
     * This class hold important player informations, which will be restored on duel end.
     */
    private static class PlayerCondition {
        private Player _player;

        private double _hp;
        private double _mp;
        private double _cp;

        private int _x;
        private int _y;
        private int _z;

        private List<AbstractEffect> _debuffs;

        public PlayerCondition(Player player, boolean partyDuel) {
            if (player == null) {
                return;
            }

            _player = player;
            _hp = _player.getStatus().getHp();
            _mp = _player.getStatus().getMp();
            _cp = _player.getStatus().getCp();

            if (partyDuel) {
                _x = _player.getX();
                _y = _player.getY();
                _z = _player.getZ();
            }
        }

        public void restoreCondition(boolean abnormalEnd) {
            teleportBack();

            if (abnormalEnd) {
                return;
            }

            _player.getStatus().setCpHpMp(_cp, _hp, _mp);

            if (_debuffs != null) {
                for (AbstractEffect effect : _debuffs) {
                    if (effect != null) {
                        effect.exit();
                    }
                }
            }
        }

        public void registerDebuff(AbstractEffect effect) {
            if (_debuffs == null) {
                _debuffs = new CopyOnWriteArrayList<>();
            }

            _debuffs.add(effect);
        }

        public void teleportBack() {
            if (_x != 0 && _y != 0) {
                _player.teleportTo(_x, _y, _z, 0);
            }
        }

        public Player getPlayer() {
            return _player;
        }
    }

    /**
     * This task makes the countdown, both for party and 1vs1 cases.
     * <ul>
     * <li>For 1vs1, the timer begins to 5 (messages then start duel process).</li>
     * <li>For party duel, the timer begins to 35 (2sec break, teleport parties, 3sec break, messages then start duel process).</li>
     * </ul>
     * The task is running until countdown reaches -1 (0 being startDuel).
     */
    private class StartTask implements Runnable {
        public StartTask() {

        }

        @Override
        public void run() {
            // Schedule anew, until time reaches 0.
            if (_countdown < 0) {
                _startTask.cancel(true);
                _startTask = null;
            }

            switch (_countdown) {
                case 60:
                    if (isMortalCombat) {
                        _playerA.sendPacket(new SocialAction(_playerA, 4));
                        _playerB.sendPacket(new SocialAction(_playerB, 4));
                    }
                    break;

                case 33:
                    if (!isMortalCombat) {
                        teleportPlayers(-83760, -238825, -3331);
                    }
                    break;

                case 30:
                case 20:
                case 15:
                case 10:
                case 5:
                case 4:
                case 3:
                case 2:
                case 1:
                    SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_countdown);
                    broadcastToTeam1(sm);
                    broadcastToTeam2(sm);
                    break;

                case 0:
                    sm = SystemMessage.getSystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
                    broadcastToTeam1(sm);
                    broadcastToTeam2(sm);

                    startDuel();
                    break;
            }

            // Decrease timer.
            _countdown--;
        }
    }

    /**
     * This task listens the different ways to disturb the duel. Two cases are possible :
     * <ul>
     * <li>DuelResult is under CONTINUE state, nothing happens. The task will continue to run every second.</li>
     * <li>DuelResult is anything except CONTINUE, then the duel ends. Animations are played on any duel end cases, except CANCELED.</li>
     * </ul>
     */
    private class CheckTask implements Runnable {
        public CheckTask() {

        }

        @Override
        public void run() {
            final DuelResult status = checkEndDuelCondition();

            if (status != DuelResult.CONTINUE) {
                // Abort the start task if it was currently running. Interrupt it, even if it was on a loop.
                if (_startTask != null) {
                    _startTask.cancel(true);
                    _startTask = null;
                }

                // Abort the check task. Let this last loop alive.
                if (_checkTask != null) {
                    _checkTask.cancel(false);
                    _checkTask = null;
                }

                if (_isPartyDuel) {
                    for (Player member : _playerA.getParty().getMembers()) {
                        stopFighting(member);
                    }

                    for (Player member : _playerB.getParty().getMembers()) {
                        stopFighting(member);
                    }
                } else {
                    stopFighting(_playerA);
                    stopFighting(_playerB);
                }

                if (status != DuelResult.CANCELED) {
                    playAnimations();
                }

                endDuel(status);
            }
        }
    }

    private static void stopFighting(Player player) {
        player.getCast().stop();
        player.getAI().tryToActive();
        player.setTarget(null);
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    /**
     * Starts the duel.<br> Save players conditions, cancel active trade, set the team color and all duel start
     * packets.<br> Handle the duel task, which checks if the duel ends in one way or another.
     */
    protected void startDuel() {
        if (_isPartyDuel) {
            for (Player partyPlayer : _playerA.getParty().getMembers()) {
                partyPlayer.cancelActiveTrade();
                partyPlayer.setDuelState(DuelState.DUELLING);
                partyPlayer.setTeam(TeamType.BLUE);
                partyPlayer.broadcastUserInfo();

                final Summon summon = partyPlayer.getSummon();
                if (summon != null) {
                    summon.updateAbnormalEffect();
                }

                broadcastToTeam2(new ExDuelUpdateUserInfo(partyPlayer));
            }

            for (Player partyPlayer : _playerB.getParty().getMembers()) {
                partyPlayer.cancelActiveTrade();
                partyPlayer.setDuelState(DuelState.DUELLING);
                partyPlayer.setTeam(TeamType.RED);
                partyPlayer.broadcastUserInfo();

                final Summon summon = partyPlayer.getSummon();
                if (summon != null) {
                    summon.updateAbnormalEffect();
                }

                broadcastToTeam1(new ExDuelUpdateUserInfo(partyPlayer));
            }

            // Send duel Start packets.
            ExDuelReady ready = new ExDuelReady(true);
            ExDuelStart start = new ExDuelStart(true);

            broadcastToTeam1(ready);
            broadcastToTeam2(ready);
            broadcastToTeam1(start);
            broadcastToTeam2(start);
        } else {
            // Set states.
            _playerA.setDuelState(DuelState.DUELLING);
            _playerA.setTeam(TeamType.BLUE);
            _playerB.setDuelState(DuelState.DUELLING);
            _playerB.setTeam(TeamType.RED);

            // Send duel Start packets.
            ExDuelReady ready = new ExDuelReady(false);
            ExDuelStart start = new ExDuelStart(false);

            broadcastToTeam1(ready);
            broadcastToTeam2(ready);
            broadcastToTeam1(start);
            broadcastToTeam2(start);

            broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
            broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));

            _playerA.broadcastUserInfo();

            Summon summon = _playerA.getSummon();
            if (summon != null) {
                summon.updateAbnormalEffect();
            }

            _playerB.broadcastUserInfo();

            summon = _playerB.getSummon();
            if (summon != null) {
                summon.updateAbnormalEffect();
            }
        }

        // Play sound.
        broadcastToTeam1(B04_S01);
        broadcastToTeam2(B04_S01);
    }

    /**
     * Save the current player condition: hp, mp, cp, location
     */
    private void savePlayerConditions() {
        if (_isPartyDuel) {
            for (Player partyPlayer : _playerA.getParty().getMembers()) {
                _playerConditions.add(new PlayerCondition(partyPlayer, _isPartyDuel));
            }

            for (Player partyPlayer : _playerB.getParty().getMembers()) {
                _playerConditions.add(new PlayerCondition(partyPlayer, _isPartyDuel));
            }
        } else {
            _playerConditions.add(new PlayerCondition(_playerA, _isPartyDuel));
            _playerConditions.add(new PlayerCondition(_playerB, _isPartyDuel));
        }
    }

    /**
     * Restore player conditions.
     *
     * @param abnormalEnd : true if the duel was canceled.
     */
    private void restorePlayerConditions(boolean abnormalEnd) {
        if (_isPartyDuel) {
            for (Player partyPlayer : _playerA.getParty().getMembers()) {
                partyPlayer.setInDuel(0);
                partyPlayer.setTeam(TeamType.NONE);
                partyPlayer.broadcastUserInfo();

                final Summon summon = partyPlayer.getSummon();
                if (summon != null) {
                    summon.updateAbnormalEffect();
                }
            }

            for (Player partyPlayer : _playerB.getParty().getMembers()) {
                partyPlayer.setInDuel(0);
                partyPlayer.setTeam(TeamType.NONE);
                partyPlayer.broadcastUserInfo();

                final Summon summon = partyPlayer.getSummon();
                if (summon != null) {
                    summon.updateAbnormalEffect();
                }
            }
        } else {
            _playerA.setInDuel(0);
            _playerA.setTeam(TeamType.NONE);
            _playerA.broadcastUserInfo();

            Summon summon = _playerA.getSummon();
            if (summon != null) {
                summon.updateAbnormalEffect();
            }

            _playerB.setInDuel(0);
            _playerB.setTeam(TeamType.NONE);
            _playerB.broadcastUserInfo();

            summon = _playerB.getSummon();
            if (summon != null) {
                summon.updateAbnormalEffect();
            }
        }

        // Restore player conditions, but only for party duel (no matter the end) && 1vs1 which ends normally.
        if (_isPartyDuel || !abnormalEnd) {
            for (PlayerCondition cond : _playerConditions) {
                cond.restoreCondition(abnormalEnd);
            }
        }
    }

    /**
     * @return the duel id.
     */
    public int getId() {
        return _duelId;
    }

    /**
     * @return the remaining time.
     */
    public int getRemainingTime() {
        return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
    }

    /**
     * @return the player that requested the duel.
     */
    public Player getPlayerA() {
        return _playerA;
    }

    /**
     * @return the player that was challenged.
     */
    public Player getPlayerB() {
        return _playerB;
    }

    /**
     * @return true if the duel was a party duel, false otherwise.
     */
    public boolean isPartyDuel() {
        return _isPartyDuel;
    }

    /**
     * Teleport all players to the given coordinates. Used by party duel only.
     *
     * @param x
     * @param y
     * @param z
     */
    protected void teleportPlayers(int x, int y, int z) {
        // TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
        if (!_isPartyDuel) {
            return;
        }

        int offset = 0;

        for (Player partyPlayer : _playerA.getParty().getMembers()) {
            partyPlayer.teleportTo(x + offset - 180, y - 150, z, 0);
            offset += 40;
        }

        offset = 0;
        for (Player partyPlayer : _playerB.getParty().getMembers()) {
            partyPlayer.teleportTo(x + offset - 180, y + 150, z, 0);
            offset += 40;
        }
    }

    /**
     * Broadcast a packet to the challenger team.
     *
     * @param packet : The packet to send.
     */
    public void broadcastToTeam1(L2GameServerPacket packet) {
        if (_isPartyDuel && _playerA.getParty() != null) {
            for (Player partyPlayer : _playerA.getParty().getMembers()) {
                partyPlayer.sendPacket(packet);
            }
        } else {
            _playerA.sendPacket(packet);
        }
    }

    /**
     * Broadcast a packet to the challenged team.
     *
     * @param packet : The packet to send.
     */
    public void broadcastToTeam2(L2GameServerPacket packet) {
        if (_isPartyDuel && _playerB.getParty() != null) {
            for (Player partyPlayer : _playerB.getParty().getMembers()) {
                partyPlayer.sendPacket(packet);
            }
        } else {
            _playerB.sendPacket(packet);
        }
    }

    /**
     * Playback the bow animation for loosers, victory pose for winners.<br> The method works even if other side is null
     * or offline.
     */
    protected void playAnimations() {
        if (_playerA.isOnline()) {
            if (_playerA.getDuelState() == DuelState.WINNER) {
                if (_isPartyDuel && _playerA.getParty() != null) {
                    for (Player partyPlayer : _playerA.getParty().getMembers()) {
                        partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 3));
                    }
                } else {
                    _playerA.broadcastPacket(new SocialAction(_playerA, 3));
                }
            } else if (_playerA.getDuelState() == DuelState.DEAD) {
                if (_isPartyDuel && _playerA.getParty() != null) {
                    for (Player partyPlayer : _playerA.getParty().getMembers()) {
                        partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 7));
                    }
                } else {
                    if (!isMortalCombat) {
                        _playerA.broadcastPacket(new SocialAction(_playerA, 7));
                    }
                    // if defeated player was in mortal combat, we don't need to send social action
                }
            }
        }

        if (_playerB.isOnline()) {
            if (_playerB.getDuelState() == DuelState.WINNER) {
                if (_isPartyDuel && _playerB.getParty() != null) {
                    for (Player partyPlayer : _playerB.getParty().getMembers()) {
                        partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 3));
                    }
                } else {
                    _playerB.broadcastPacket(new SocialAction(_playerB, 3));
                }
            } else if (_playerB.getDuelState() == DuelState.DEAD) {
                if (_isPartyDuel && _playerB.getParty() != null) {
                    for (Player partyPlayer : _playerB.getParty().getMembers()) {
                        partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 7));
                    }
                } else {
                    _playerB.broadcastPacket(new SocialAction(_playerB, 7));
                }
            }
        }
    }

    /**
     * This method ends a duel, sending messages to each team, end duel packet, cleaning player conditions and then
     * removing duel from manager.
     *
     * @param result : The duel result.
     */
    protected void endDuel(DuelResult result) {
        SystemMessage sm = null;
        switch (result) {
            case TEAM_2_SURRENDER:
                sm = SystemMessage.getSystemMessage((_isPartyDuel) ? SystemMessageId.SINCE_S1_PARTY_WITHDREW_FROM_THE_DUEL_S2_PARTY_HAS_WON : SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName());
                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
            case TEAM_1_WIN:
                sm = SystemMessage.getSystemMessage((_isPartyDuel) ? SystemMessageId.S1_PARTY_HAS_WON_THE_DUEL : SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerA.getName());
                break;

            case TEAM_1_SURRENDER:
                sm = SystemMessage.getSystemMessage((_isPartyDuel) ? SystemMessageId.SINCE_S1_PARTY_WITHDREW_FROM_THE_DUEL_S2_PARTY_HAS_WON : SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName());
                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
            case TEAM_2_WIN:
                sm = SystemMessage.getSystemMessage((_isPartyDuel) ? SystemMessageId.S1_PARTY_HAS_WON_THE_DUEL : SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerB.getName());
                break;

            case CANCELED:
            case TIMEOUT:
                sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
                break;
        }

        broadcastToTeam1(sm);
        broadcastToTeam2(sm);

        restorePlayerConditions(result == DuelResult.CANCELED);

        // Send end duel packet.
        ExDuelEnd duelEnd = new ExDuelEnd(_isPartyDuel);

        broadcastToTeam1(duelEnd);
        broadcastToTeam2(duelEnd);

        // Cleanup.
        _playerConditions.clear();
        DuelManager.getInstance().removeDuel(_duelId);
    }

    /**
     * This method checks all possible scenari which can disturb a duel, and return the appropriate status.
     *
     * @return DuelResult : The duel status.
     */
    protected DuelResult checkEndDuelCondition() {
        // Both players are offline.
        if (!_playerA.isOnline() && !_playerB.isOnline()) {
            return DuelResult.CANCELED;
        }

        // Player A is offline.
        if (!_playerA.isOnline()) {
            onPlayerDefeat(_playerA);
            return DuelResult.TEAM_1_SURRENDER;
        }

        // Player B is offline.
        if (!_playerB.isOnline()) {
            onPlayerDefeat(_playerB);
            return DuelResult.TEAM_2_SURRENDER;
        }

        // Duel surrender request.
        if (_surrenderRequest != 0) {
            return (_surrenderRequest == 1) ? DuelResult.TEAM_1_SURRENDER : DuelResult.TEAM_2_SURRENDER;
        }

        // Duel timed out.
        if (getRemainingTime() <= 0) {
            return DuelResult.TIMEOUT;
        }

        // One of the players is declared winner.
        if (_playerA.getDuelState() == DuelState.WINNER) {
            return DuelResult.TEAM_1_WIN;
        }

        if (_playerB.getDuelState() == DuelState.WINNER) {
            return DuelResult.TEAM_2_WIN;
        }

        if (!_isPartyDuel) {
            // Duel was interrupted e.g.: player was attacked by mobs / other players
            if (_playerA.getDuelState() == DuelState.INTERRUPTED || _playerB.getDuelState() == DuelState.INTERRUPTED) {
                return DuelResult.CANCELED;
            }

            // Players are too far apart.
            if (!_playerA.isIn3DRadius(_playerB, 2000)) {
                return DuelResult.CANCELED;
            }

            // One of the players is engaged in PvP.
            if (_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0) {
                return DuelResult.CANCELED;
            }

            // One of the players is in a Siege, Peace or PvP zone.
            if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP)) {
                return DuelResult.CANCELED;
            }
        } else {
            if (_playerA.getParty() != null) {
                for (Player partyMember : _playerA.getParty().getMembers()) {
                    // Duel was interrupted e.g.: player was attacked by mobs / other players
                    if (partyMember.getDuelState() == DuelState.INTERRUPTED) {
                        return DuelResult.CANCELED;
                    }

                    // Players are too far apart.
                    if (!partyMember.isIn3DRadius(_playerB, 2000)) {
                        return DuelResult.CANCELED;
                    }

                    // One of the players is engaged in PvP.
                    if (partyMember.getPvpFlag() != 0) {
                        return DuelResult.CANCELED;
                    }

                    // One of the players is in a Siege, Peace or PvP zone.
                    if (partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.PVP) || partyMember.isInsideZone(ZoneId.SIEGE)) {
                        return DuelResult.CANCELED;
                    }
                }
            }

            if (_playerB.getParty() != null) {
                for (Player partyMember : _playerB.getParty().getMembers()) {
                    // Duel was interrupted e.g.: player was attacked by mobs / other players
                    if (partyMember.getDuelState() == DuelState.INTERRUPTED) {
                        return DuelResult.CANCELED;
                    }

                    // Players are too far apart.
                    if (!partyMember.isIn3DRadius(_playerA, 2000)) {
                        return DuelResult.CANCELED;
                    }

                    // One of the players is engaged in PvP.
                    if (partyMember.getPvpFlag() != 0) {
                        return DuelResult.CANCELED;
                    }

                    // One of the players is in a Siege, Peace or PvP zone.
                    if (partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.PVP) || partyMember.isInsideZone(ZoneId.SIEGE)) {
                        return DuelResult.CANCELED;
                    }
                }
            }
        }

        return DuelResult.CONTINUE;
    }

    /**
     * Register a surrender request. It updates DuelState of players.
     *
     * @param player : The player who surrenders.
     */
    public void doSurrender(Player player) {
        // A surrender request is already under process, return.
        if (_surrenderRequest != 0) {
            return;
        }

        // TODO: Can every party member cancel a party duel? or only the party leaders?
        if (_isPartyDuel) {
            if (_playerA.getParty().containsPlayer(player)) {
                _surrenderRequest = 1;

                for (Player partyPlayer : _playerA.getParty().getMembers()) {
                    partyPlayer.setDuelState(DuelState.DEAD);
                }

                for (Player partyPlayer : _playerB.getParty().getMembers()) {
                    partyPlayer.setDuelState(DuelState.WINNER);
                }
            } else if (_playerB.getParty().containsPlayer(player)) {
                _surrenderRequest = 2;

                for (Player partyPlayer : _playerB.getParty().getMembers()) {
                    partyPlayer.setDuelState(DuelState.DEAD);
                }

                for (Player partyPlayer : _playerA.getParty().getMembers()) {
                    partyPlayer.setDuelState(DuelState.WINNER);
                }
            }
        } else {
            if (player == _playerA) {
                _surrenderRequest = 1;

                _playerA.setDuelState(DuelState.DEAD);
                _playerB.setDuelState(DuelState.WINNER);
            } else if (player == _playerB) {
                _surrenderRequest = 2;

                _playerB.setDuelState(DuelState.DEAD);
                _playerA.setDuelState(DuelState.WINNER);
            }
        }
    }

    /**
     * This method is called whenever a player was defeated in a duel. It updates DuelState of players.
     *
     * @param player : The defeated player.
     */
    public void onPlayerDefeat(Player player) {
        // Set player as defeated.
        player.setDuelState(DuelState.DEAD);

        if (_isPartyDuel) {
            boolean teamDefeated = true;
            for (Player partyPlayer : player.getParty().getMembers()) {
                if (partyPlayer.getDuelState() == DuelState.DUELLING) {
                    teamDefeated = false;
                    break;
                }
            }

            if (teamDefeated) {
                Player winner = _playerA;
                if (_playerA.getParty().containsPlayer(player)) {
                    winner = _playerB;
                }

                for (Player partyPlayer : winner.getParty().getMembers()) {
                    partyPlayer.setDuelState(DuelState.WINNER);
                }
            }
        } else {
            if (_playerA == player) {
                _playerB.setDuelState(DuelState.WINNER);
            } else {
                _playerA.setDuelState(DuelState.WINNER);
            }

            if (isMortalCombat) {
                player.setDieReason(DieReason.MORTAL_COMBAT);
                player.doDie(player == _playerA ? _playerB : _playerA);
            }
        }
    }

    /**
     * This method is called when a player join/leave a party during a Duel, and enforce Duel cancellation.
     */
    public void onPartyEdit() {
        if (!_isPartyDuel) {
            return;
        }

        // Teleport back players, setting their duelId to 0.
        for (PlayerCondition cond : _playerConditions) {
            cond.teleportBack();
            cond.getPlayer().setInDuel(0);
        }

        // Cancel the duel properly.
        endDuel(DuelResult.CANCELED);
    }

    /**
     * This method is called to register an effect.
     *
     * @param player : The player condition to affect.
     * @param effect : The effect to register.
     */
    public void onBuff(Player player, AbstractEffect effect) {
        for (PlayerCondition cond : _playerConditions) {
            if (cond.getPlayer() == player) {
                cond.registerDebuff(effect);
                return;
            }
        }
    }
}