package net.sf.l2j.gameserver.data.sql;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.manager.DayNightManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.Spawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SpawnTable {

    private static final String LOAD_SPAWNS = "SELECT * FROM spawnlist";
    private static final String ADD_SPAWN = "INSERT INTO spawnlist (npc_templateid,locx,locy,locz,heading,respawn_delay) values(?,?,?,?,?,?)";
    private static final String DELETE_SPAWN = "DELETE FROM spawnlist WHERE locx=? AND locy=? AND locz=? AND npc_templateid=? AND heading=?";

    private final Set<Spawn> _spawns = ConcurrentHashMap.newKeySet();

    protected SpawnTable() {
        if (!Config.NO_SPAWNS) {
            load();
        }
    }

    private void load() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(LOAD_SPAWNS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final NpcTemplate template = NpcData.getInstance().getTemplate(rs.getInt("npc_templateid"));
                if (template == null) {
                    log.warn("Invalid template {} found on spawn load.", rs.getInt("npc_templateid"));
                    continue;
                }

                if (template.isType("SiegeGuard")) {
                    // Don't spawn guards, they're spawned during castle sieges.
                } else if (template.isType("RaidBoss")) {
                    // Don't spawn raidbosses ; raidbosses are supposed to be loaded in another table !
                    log.warn("RB template {} is in regular spawnlist, move it in raidboss_spawnlist.", template.getIdTemplate());
                } else if (!Config.ALLOW_CLASS_MASTERS && template.isType("ClassMaster")) {
                    // Dont' spawn class masters (if config is setuped to false).
                } else if (!Config.WYVERN_ALLOW_UPGRADER && template.isType("WyvernManagerNpc")) {
                    // Dont' spawn wyvern managers (if config is setuped to false).
                } else {
                    final Spawn spawnDat = new Spawn(template);
                    spawnDat.setLoc(rs.getInt("locx"), rs.getInt("locy"), rs.getInt("locz"), rs.getInt("heading"));
                    spawnDat.setRespawnDelay(rs.getInt("respawn_delay"));
                    spawnDat.setRespawnRandom(rs.getInt("respawn_rand"));

                    switch (rs.getInt("periodOfDay")) {
                        case 0: // default
                            spawnDat.setRespawnState(true);
                            spawnDat.doSpawn(false);
                            break;

                        case 1: // Day
                            DayNightManager.getInstance().addDayCreature(spawnDat);
                            break;

                        case 2: // Night
                            DayNightManager.getInstance().addNightCreature(spawnDat);
                            break;
                    }

                    _spawns.add(spawnDat);
                }
            }
        } catch (Exception e) {
            log.error("Couldn't load spawns.", e);
        }

        log.info("Loaded {} spawns.", _spawns.size());
    }

    public void reload() {
        _spawns.clear();

        load();
    }

    public Set<Spawn> getSpawns() {
        return _spawns;
    }

    public void addSpawn(Spawn spawn, boolean storeInDb) {
        _spawns.add(spawn);

        if (storeInDb) {
            try (Connection con = ConnectionPool.getConnection();
                 PreparedStatement ps = con.prepareStatement(ADD_SPAWN)) {
                ps.setInt(1, spawn.getNpcId());
                ps.setInt(2, spawn.getLocX());
                ps.setInt(3, spawn.getLocY());
                ps.setInt(4, spawn.getLocZ());
                ps.setInt(5, spawn.getHeading());
                ps.setInt(6, spawn.getRespawnDelay());
                ps.execute();
            } catch (Exception e) {
                log.error("Couldn't add spawn.", e);
            }
        }
    }

    public void deleteSpawn(Spawn spawn, boolean updateDb) {
        if (!_spawns.remove(spawn)) {
            return;
        }

        if (updateDb) {
            try (Connection con = ConnectionPool.getConnection();
                 PreparedStatement ps = con.prepareStatement(DELETE_SPAWN)) {
                ps.setInt(1, spawn.getLocX());
                ps.setInt(2, spawn.getLocY());
                ps.setInt(3, spawn.getLocZ());
                ps.setInt(4, spawn.getNpcId());
                ps.setInt(5, spawn.getHeading());
                ps.execute();
            } catch (Exception e) {
                log.error("Couldn't delete spawn.", e);
            }
        }
    }

    public static SpawnTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final SpawnTable INSTANCE = new SpawnTable();
    }
}