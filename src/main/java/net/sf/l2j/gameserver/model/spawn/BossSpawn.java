package net.sf.l2j.gameserver.model.spawn;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.BossStatus;
import net.sf.l2j.gameserver.model.actor.Npc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.concurrent.ScheduledFuture;

/**
 * A data holder keeping informations related to the {@link Spawn} of a RaidBoss.
 */
@Slf4j
public class BossSpawn {

    private static final String DELETE_RAIDBOSS = "DELETE FROM raidboss_spawnlist WHERE boss_id=?";
    private static final String UPDATE_RAIDBOSS = "UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?";

    private Spawn _spawn;
    private BossStatus _status = BossStatus.UNDEFINED;

    private ScheduledFuture<?> _task;

    private double _currentHp;
    private double _currentMp;

    private long _respawnTime;

    @Override
    public String toString() {
        return "BossSpawn{" + "npcId=" + _spawn.getNpcId() + ", status=" + _status + ", hp=" + _currentHp + ", mp=" + _currentMp + ", respawnTime=" + _respawnTime + "}";
    }

    public Spawn getSpawn() {
        return _spawn;
    }

    public void setSpawn(Spawn spawn) {
        _spawn = spawn;
    }

    public BossStatus getStatus() {
        return _status;
    }

    public void setStatus(BossStatus status) {
        _status = status;
    }

    public ScheduledFuture<?> getTask() {
        return _task;
    }

    public void setTask(ScheduledFuture<?> task) {
        _task = task;
    }

    /**
     * Cancel the {@link ScheduledFuture} and drop the reference, if any.
     */
    public void cancelTask() {
        if (_task != null) {
            _task.cancel(false);
            _task = null;
        }
    }

    public double getCurrentHp() {
        return _currentHp;
    }

    public void setCurrentHp(double currentHp) {
        _currentHp = currentHp;
    }

    public double getCurrentMp() {
        return _currentMp;
    }

    public void setCurrentMp(double currentMp) {
        _currentMp = currentMp;
    }

    public long getRespawnTime() {
        return _respawnTime;
    }

    public void setRespawnTime(long respawnTime) {
        _respawnTime = respawnTime;
    }

    public Npc getBoss() {
        return _spawn.getNpc();
    }

    /**
     * A method called upon {@link Npc} death.
     * <ul>
     * <li>Calculate the new respawn time, based on {@link Spawn} respawn delays.</li>
     * <li>Refresh this {@link BossSpawn} holder.</li>
     * <li>Cancel any running {@link ScheduledFuture}, and reschedule it with new respawn time.</li>
     * <li>Save informations on database.</li>
     * </ul>
     */
    public void onDeath() {
        // getRespawnMinDelay() is used as fixed timer, while getRespawnMaxDelay() is used as random timer.
        final int respawnDelay = _spawn.getRespawnMinDelay() + Rnd.get(-_spawn.getRespawnMaxDelay(), _spawn.getRespawnMaxDelay());
        final long respawnTime = System.currentTimeMillis() + (respawnDelay * 3600000);

        // Refresh data.
        _status = BossStatus.DEAD;
        _currentHp = 0;
        _currentMp = 0;
        _respawnTime = respawnTime;

        // Cancel task, if running.
        cancelTask();

        // Register the task.
        _task = ThreadPool.schedule(this::onSpawn, respawnDelay * 3600000L);

        // Refresh the database for this particular boss entry.
        updateOnDb();

        log.info("Raid boss: {} - {} ({}h).", _spawn.getNpc().getName(), new SimpleDateFormat("dd-MM-yyyy HH:mm").format(respawnTime), respawnDelay);
    }

    /**
     * A method called upon {@link Npc} spawn.
     * <ul>
     * <li>Spawn the Npc instance based on its {@link Spawn}.</li>
     * <li>Refresh this {@link BossSpawn} holder.</li>
     * <li>Cancel any running {@link ScheduledFuture}.</li>
     * <li>Save informations on database.</li>
     * </ul>
     */
    public void onSpawn() {
        final Npc npc = _spawn.doSpawn(false);

        // Refresh data.
        _status = BossStatus.ALIVE;
        _currentHp = npc.getStatus().getMaxHp();
        _currentMp = npc.getStatus().getMaxMp();
        _respawnTime = 0L;

        // Cancel task, if running.
        cancelTask();

        // Refresh the database for this particular boss entry.
        updateOnDb();

        log.info("{} raid boss has spawned.", npc.getName());
    }

    /**
     * A method called upon {@link Npc} despawn.
     * <ul>
     * <li>Cancel any running {@link ScheduledFuture}.</li>
     * <li>Delete the Npc instance based on its {@link Spawn}.</li>
     * <li>Save informations on database.</li>
     * <li>Delete the reference of Spawn from this {@link BossSpawn} holder.</li>
     * </ul>
     */
    public void onDespawn() {
        // Cancel task, if running.
        cancelTask();

        // Delete the Npc.
        final Npc npc = _spawn.getNpc();
        if (npc != null && !npc.isDecayed()) {
            npc.deleteMe();
        }

        // Refresh database.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_RAIDBOSS)) {
            ps.setInt(1, _spawn.getNpcId());
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Couldn't remove raid boss #{}.", _spawn.getNpcId(), e);
        }

        // Drop the Spawn reference.
        _spawn = null;
    }

    /**
     * Update the respawn time and current HP/MP of this {@link BossSpawn} on the database.
     */
    private void updateOnDb() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_RAIDBOSS)) {
            ps.setLong(1, _respawnTime);
            ps.setDouble(2, _currentHp);
            ps.setDouble(3, _currentMp);
            ps.setInt(4, _spawn.getNpcId());
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Couldn't update raid boss #{}.", _spawn.getNpcId(), e);
        }
    }
}