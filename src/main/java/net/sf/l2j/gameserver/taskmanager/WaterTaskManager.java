package net.sf.l2j.gameserver.taskmanager;

import lombok.Getter;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.graveyard.DieReason;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updates {@link Player} drown timer and reduces {@link Player} HP, when drowning.
 */
public class WaterTaskManager implements Runnable {

    @Getter(lazy = true)
    private static final WaterTaskManager instance = new WaterTaskManager();
    private final Map<Player, Long> players = new ConcurrentHashMap<>();

    private WaterTaskManager() {
        // Run task each second.
        ThreadPool.scheduleAtFixedRate(this, 2000, 2000);
    }

    @Override
    public void run() {
        // List is empty, skip.
        if (players.isEmpty()) {
            return;
        }

        // Get current time.
        long time = System.currentTimeMillis();

        // Loop all players.
        for (Map.Entry<Player, Long> entry : players.entrySet()) {
            // Time has not passed yet, skip.
            if (time < entry.getValue()) {
                continue;
            }

            // Get player.
            Player player = entry.getKey();

            // Reduce 1% of HP per second.
            PlayerStatus status = player.getStatus();
            double current = status.getHp();
            double max = status.getMaxHp();
            double damage = max / 100.0 * Rnd.get(5, 10);
            if (current - damage < 0.5) {
                player.setDieReason(DieReason.DROWN);
            }
            player.reduceCurrentHp(damage, player, false, false, null);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DROWN_DAMAGE_S1).addNumber((int) damage));
        }
    }

    /**
     * Adds {@link Player} to the WaterTask.
     *
     * @param player : {@link Player} to be added and checked.
     */
    public void add(Player player) {
        if (!player.isDead() && !players.containsKey(player)) {
            int time = (int) player.getStatus().calcStat(Stats.BREATH, player.getRace().getBreath(), player, null);

            players.put(player, System.currentTimeMillis() + time);

            player.sendPacket(new SetupGauge(GaugeColor.CYAN, time));
        }
    }

    /**
     * Removes {@link Player} from the WaterTask.
     *
     * @param player : Player to be removed.
     */
    public void remove(Player player) {
        if (players.remove(player) != null) {
            player.sendPacket(new SetupGauge(GaugeColor.CYAN, 0));
        }
    }

}
