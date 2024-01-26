package net.sf.l2j.gameserver.taskmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Updates {@link Player} drown timer and reduces {@link Player} HP, when drowning.
 */
public final class WaterTaskManager implements Runnable {

    @Getter(lazy = true)
    private static final WaterTaskManager instance = new WaterTaskManager();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Swimmer implements Comparable<Long> {
        public long startSwimTimestamp;
        public Player player;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Swimmer swimmer = (Swimmer) o;
            return Objects.equals(player.getObjectId(), swimmer.player.getObjectId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(player.getObjectId());
        }

        @Override
        public int compareTo(Long startSwimTimestamp) {
            return (int) (this.startSwimTimestamp - startSwimTimestamp);
        }
    }

    private final Set<Swimmer> swimmers = new ConcurrentSkipListSet<>();

    private WaterTaskManager() {
        ThreadPool.scheduleAtFixedRate(this, 2000, 2000);
    }

    @Override
    public void run() {
        // List is empty, skip.
        if (swimmers.isEmpty()) {
            return;
        }

        // Get current time.
        long time = System.currentTimeMillis();

        swimmers.stream()
            .filter(swimmer -> swimmer.startSwimTimestamp > time)
            .forEach(swimmer -> drown(swimmer.player));
    }

    private void drown(Player player) {
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

    public void add(Player player) {
        int time = (int) player.getStatus().calcStat(Stats.BREATH, player.getRace().getBreath(), player, null);
        Swimmer swimmer = Swimmer.builder()
            .startSwimTimestamp(System.currentTimeMillis() + time)
            .player(player)
            .build();

        if (swimmers.add(swimmer)) {
            player.sendPacket(new SetupGauge(GaugeColor.CYAN, time));
        }
    }

    public void remove(Player player) {
        if (swimmers.remove(Swimmer.builder().player(player).build())) {
            player.sendPacket(new SetupGauge(GaugeColor.CYAN, 0));
        }
    }

}