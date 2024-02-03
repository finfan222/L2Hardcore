package net.sf.l2j.gameserver.taskmanager;

import lombok.Getter;
import net.sf.l2j.Config;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.events.OnDie;
import net.sf.l2j.gameserver.events.OnRevalidateZone;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.graveyard.DieReason;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updates {@link Player} drown timer and reduces {@link Player} HP, when drowning.
 */
public class WaterTaskManager implements Runnable {

    static class Swimmer implements Comparable<Swimmer> {

        private static final int MIN_DAMAGE = 1;
        private static final int MAX_DAMAGE = 10;

        public Player player;
        public long timestamp;

        public Swimmer(Player player) {
            this.player = player;
            int time = (int) player.getStatus().calcStat(Stats.BREATH, player.getRace().getBreath(), player, null);
            timestamp = System.currentTimeMillis() + time;
            player.sendPacket(new SetupGauge(GaugeColor.CYAN, time));
        }

        public boolean cantBreath() {
            return System.currentTimeMillis() > timestamp;
        }

        public void drown() {
            // Reduce 3~6% of HP per 2 second.
            PlayerStatus status = player.getStatus();
            double current = status.getHp();
            double max = status.getMaxHp();
            double damage = max / 100.0 * Rnd.get(MIN_DAMAGE, MAX_DAMAGE);
            if (current - damage < 0.5) {
                player.setDieReason(DieReason.DROWN);
            }
            player.reduceCurrentHp(damage, player, false, false, null);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DROWN_DAMAGE_S1).addNumber((int) damage));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Swimmer swimmer = (Swimmer) o;
            return Objects.equals(player, swimmer.player);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player);
        }

        @Override
        public int compareTo(Swimmer o) {
            return Long.compare(timestamp, o.timestamp);
        }
    }

    @Getter(lazy = true)
    private static final WaterTaskManager instance = new WaterTaskManager();
    private static final Map<Integer, Swimmer> swimmers = new ConcurrentHashMap<>();

    private WaterTaskManager() {
        GlobalEventListener.register(OnDie.class).forEach(this::onDie);
        GlobalEventListener.register(OnRevalidateZone.class).forEach(this::onRevalidateZone);
        ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
    }

    @Override
    public void run() {
        if (swimmers.isEmpty()) {
            return;
        }

        swimmers.values().stream()
            .filter(swimmer -> !swimmer.player.isDead())
            .filter(Swimmer::cantBreath)
            .forEach(Swimmer::drown);
    }

    public void remove(Player player) {
        swimmers.remove(player.getObjectId());
    }

    ///////////////////
    // Event Handlers
    ///////////////////

    private void onDie(OnDie event) {
        swimmers.remove(event.getVictim().getObjectId());
    }

    private void onRevalidateZone(OnRevalidateZone event) {
        Player player = event.getPlayer();
        if (Config.ALLOW_WATER) {
            if (player.isInWater()) {
                if (player.isDead()) {
                    return;
                }

                if (!swimmers.containsKey(player.getObjectId())) {
                    swimmers.put(player.getObjectId(), new Swimmer(player));
                }
            } else {
                swimmers.remove(player.getObjectId());
            }
        }
    }
}
