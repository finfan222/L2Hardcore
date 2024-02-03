package net.sf.l2j.gameserver.data.manager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and stores {@link Duel}s for easier management.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DuelManager {

    @Getter(lazy = true)
    private static final DuelManager instance = new DuelManager();

    private final Map<Integer, Duel> duels = new ConcurrentHashMap<>();

    public Duel getDuel(int duelId) {
        return duels.get(duelId);
    }

    /**
     * Add a Duel on the _duels Map. Both players must exist.
     *
     * @param playerA : The first player to use.
     * @param playerB : The second player to use.
     * @param isPartyDuel : True if the duel is a party duel.
     */
    public void addDuel(Player playerA, Player playerB, boolean isPartyDuel, boolean isMortalCombat) {
        if (playerA == null || playerB == null) {
            return;
        }

        // Compute a new id.
        final int duelId = IdFactory.getInstance().getNextId();

        // Feed the Map.
        duels.put(duelId, new Duel(playerA, playerB, isPartyDuel, duelId, isMortalCombat));
    }

    /**
     * Remove the duel from the Map, and release the id.
     *
     * @param duelId : The id to remove.
     */
    public void removeDuel(int duelId) {
        // Release the id.
        IdFactory.getInstance().releaseId(duelId);

        // Delete from the Map.
        duels.remove(duelId);
    }

    /**
     * Ends the duel by a surrender action.
     *
     * @param player : The player used to retrieve the duelId. The player is then used as surrendered opponent.
     */
    public void doSurrender(Player player) {
        if (player == null || !player.isInDuel()) {
            return;
        }

        final Duel duel = getDuel(player.getDuelId());
        if (duel != null) {
            if (!duel.isMortalCombat()) {
                duel.doSurrender(player);
            } else {
                player.sendMessage("Правила смертельной битвы запрещают сдачу.");
            }
        }
    }

    /**
     * Ends the duel by a defeat action.
     *
     * @param player : The player used to retrieve the duelId. The player is then used as defeated opponent.
     */
    public void onPlayerDefeat(Player player) {
        if (player == null || !player.isInDuel()) {
            return;
        }

        final Duel duel = getDuel(player.getDuelId());
        if (duel != null) {
            duel.onPlayerDefeat(player);
        }
    }

    /**
     * Registers a buff which will be removed if the duel ends.
     *
     * @param player : The player to buff.
     * @param buff : The effect to cast.
     */
    public void onBuff(Player player, AbstractEffect buff) {
        if (player == null || !player.isInDuel() || buff == null) {
            return;
        }

        final Duel duel = getDuel(player.getDuelId());
        if (duel != null) {
            duel.onBuff(player, buff);
        }
    }

    /**
     * Removes player from duel, enforcing duel cancellation.
     *
     * @param player : The player to check.
     */
    public void onPartyEdit(Player player) {
        if (player == null || !player.isInDuel()) {
            return;
        }

        final Duel duel = getDuel(player.getDuelId());
        if (duel != null) {
            duel.onPartyEdit();
        }
    }

    /**
     * Broadcasts a packet to the team (or the player) opposing the given player.
     *
     * @param player : The player used to find the opponent.
     * @param packet : The packet to send.
     */
    public void broadcastToOppositeTeam(Player player, L2GameServerPacket packet) {
        if (player == null || !player.isInDuel()) {
            return;
        }

        final Duel duel = getDuel(player.getDuelId());
        if (duel == null) {
            return;
        }

        if (duel.getPlayerA() == player) {
            duel.broadcastToTeam2(packet);
        } else if (duel.getPlayerB() == player) {
            duel.broadcastToTeam1(packet);
        } else if (duel.isPartyDuel()) {
            if (duel.getPlayerA().getParty() != null && duel.getPlayerA().getParty().containsPlayer(player)) {
                duel.broadcastToTeam2(packet);
            } else if (duel.getPlayerB().getParty() != null && duel.getPlayerB().getParty().containsPlayer(player)) {
                duel.broadcastToTeam1(packet);
            }
        }
    }

    /**
     * Request call to a {@link Player} target for mortal combat duel to the death.
     *
     * @param player requester
     * @param opponent receiver
     */
    public void requestMortalCombat(Player player, Player opponent) {
        if (player.getStatus().getLevel() < 15) {
            player.sendMessage("Этот функционал откроется на 15-ом уровне");
            player.sendPacket(SystemMessageId.INVALID_TARGET);
            return;
        }

        if (!player.canDuel()) {
            player.sendPacket(SystemMessageId.INVALID_TARGET);
            return;
        }

        if (opponent.isDead()
            || opponent.isInDuel()
            || opponent.isOperating()
            || opponent.isInOlympiadMode()
            || !opponent.canDuel()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (opponent.getStatus().getLevel() < 15) {
            player.sendMessage(String.format("%s не обладает правом участвовать в смертельных битвах.", opponent.getName()));
            player.sendPacket(SystemMessageId.INVALID_TARGET);
            return;
        }

        if (opponent.getDialog() != null || AttackStanceTaskManager.getInstance().isInAttackStance(opponent)) {
            player.sendMessage(String.format("%s сейчас занят и не может принять дуэль до смерти.", opponent));
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_CALLS_YOU_TO_A_MORTAL_COMBAT)
            .addTime(30000)
            .addCharName(player);
        opponent.setDialog(new Dialog(opponent, dlg, Map.of("requester", player)).send());
    }

}