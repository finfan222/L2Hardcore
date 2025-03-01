package net.sf.l2j.gameserver.data.manager;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.item.MercenaryTicket;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.location.TowerSpawnLocation;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.zone.type.SiegeZone;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and stores {@link Castle}s informations, using database and XML informations.
 */
public final class CastleManager implements IXmlReader {
    private static final String LOAD_CASTLES = "SELECT * FROM castle ORDER BY id";
    private static final String LOAD_OWNER = "SELECT clan_id FROM clan_data WHERE hasCastle=?";
    private static final String RESET_CERTIFICATES = "UPDATE castle SET certificates=300";

    private final Map<Integer, Castle> _castles = new HashMap<>();

    protected CastleManager() {
        // Generate Castle objects with dynamic data.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(LOAD_CASTLES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final int id = rs.getInt("id");
                final Castle castle = new Castle(id, rs.getString("name"));

                castle.setSiegeDate(Calendar.getInstance());
                castle.getSiegeDate().setTimeInMillis(rs.getLong("siegeDate"));
                castle.setTimeRegistrationOver(rs.getBoolean("regTimeOver"));
                castle.setTaxPercent(rs.getInt("taxPercent"), false);
                castle.setTreasury(rs.getLong("treasury"));
                castle.setLeftCertificates(rs.getInt("certificates"), false);

                try (PreparedStatement ps1 = con.prepareStatement(LOAD_OWNER)) {
                    ps1.setInt(1, id);
                    try (ResultSet rs1 = ps1.executeQuery()) {
                        while (rs1.next()) {
                            final int ownerId = rs1.getInt("clan_id");
                            if (ownerId > 0) {
                                final Clan clan = ClanTable.getInstance().getClan(ownerId);
                                if (clan != null) {
                                    castle.setOwnerId(ownerId);
                                }
                            }
                        }
                    }
                }

                _castles.put(id, castle);
            }
        } catch (Exception e) {
            log.error("Failed to load castles.", e);
        }

        // Feed Castle objects with static data.
        load();

        // Load traps informations. Generate siege entities for every castle (if not handled, it's only processed during player login).
        for (Castle castle : _castles.values()) {
            castle.loadTrapUpgrade();
            castle.setSiege(new Siege(castle));
        }
    }

    @Override
    public void load() {
        parseFile("./data/xml/castles.xml");
        log.info("Loaded {} castles.", _castles.size());
    }

    @Override
    public void parseDocument(Document doc, Path path) {
        forEach(doc, "list", listNode -> forEach(listNode, "castle", castleNode ->
        {
            final NamedNodeMap attrs = castleNode.getAttributes();
            final Castle castle = _castles.get(parseInteger(attrs, "id"));
            if (castle != null) {
                castle.setCircletId(parseInteger(attrs, "circletId"));
                forEach(castleNode, "artifact", artifactNode -> castle.setArtifacts(parseString(artifactNode.getAttributes(), "val")));
                forEach(castleNode, "controlTowers", controlTowersNode -> forEach(controlTowersNode, "tower", towerNode ->
                {
                    final String[] location = parseString(towerNode.getAttributes(), "loc").split(",");
                    castle.getControlTowers().add(new TowerSpawnLocation(13002, new SpawnLocation(Integer.parseInt(location[0]), Integer.parseInt(location[1]), Integer.parseInt(location[2]), -1)));
                }));
                forEach(castleNode, "flameTowers", flameTowersNode -> forEach(flameTowersNode, "tower", towerNode ->
                {
                    final NamedNodeMap towerAttrs = towerNode.getAttributes();
                    final String[] location = parseString(towerAttrs, "loc").split(",");
                    castle.getFlameTowers().add(new TowerSpawnLocation(13004, new SpawnLocation(Integer.parseInt(location[0]), Integer.parseInt(location[1]), Integer.parseInt(location[2]), -1), parseString(towerAttrs, "zones").split(",")));
                }));
                forEach(castleNode, "relatedNpcIds", relatedNpcIdsNode -> castle.setRelatedNpcIds(parseString(relatedNpcIdsNode.getAttributes(), "val")));
                forEach(castleNode, "spawns", spawnsNode -> forEach(spawnsNode, "spawn", spawnNode -> castle.addSpawn(parseEnum(spawnNode.getAttributes(), SpawnType.class, "type"), parseLocation(spawnNode))));
                forEach(castleNode, "tickets", ticketsNode -> forEach(ticketsNode, "ticket", ticketNode -> castle.getTickets().add(new MercenaryTicket(parseAttributes(ticketNode)))));
            }
        }));
    }

    public Castle getCastleById(int castleId) {
        return _castles.get(castleId);
    }

    public Castle getCastleByOwner(Clan clan) {
        return _castles.values().stream().filter(c -> c.getOwnerId() == clan.getClanId()).findFirst().orElse(null);
    }

    public Castle getCastleByName(String name) {
        return _castles.values().stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Castle getCastle(int x, int y, int z) {
        return _castles.values().stream().filter(c -> c.getSiegeZone().isInsideZone(x, y, z)).findFirst().orElse(null);
    }

    public Castle getCastle(WorldObject object) {
        return getCastle(object.getX(), object.getY(), object.getZ());
    }

    public Collection<Castle> getCastles() {
        return _castles.values();
    }

    public void validateTaxes(CabalType sealStrifeOwner) {
        int maxTax;
        switch (sealStrifeOwner) {
            case DAWN:
                maxTax = 25;
                break;

            case DUSK:
                maxTax = 5;
                break;

            default:
                maxTax = 15;
                break;
        }

        _castles.values().stream().filter(c -> c.getTaxPercent() > maxTax).forEach(c -> c.setTaxPercent(maxTax, true));
    }

    /**
     * @param object : The {@link WorldObject} to check.
     * @return True if the {@link WorldObject} set as parameter is inside an ACTIVE {@link SiegeZone}, or false
     * otherwise.
     */
    public Siege getActiveSiege(WorldObject object) {
        return getActiveSiege(object.getX(), object.getY(), object.getZ());
    }

    /**
     * @param x : The X coord to test.
     * @param y : The Y coord to test.
     * @param z : The Z coord to test.
     * @return True if coords are set inside an ACTIVE {@link SiegeZone}, or false otherwise.
     */
    public Siege getActiveSiege(int x, int y, int z) {
        for (Castle castle : _castles.values()) {
            final Siege siege = castle.getSiege();
            if (siege.isInProgress() && castle.getSiegeZone().isInsideZone(x, y, z)) {
                return siege;
            }
        }
        return null;
    }

    /**
     * Reset all castles certificates. Reset the memory value, and run a unique query.
     */
    public void resetCertificates() {
        // Reset memory. Don't use the inner save.
        for (Castle castle : _castles.values()) {
            castle.setLeftCertificates(300, false);
        }

        // Update all castles with a single query.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESET_CERTIFICATES)) {
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Failed to reset certificates.", e);
        }
    }

    public static final CastleManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final class SingletonHolder {
        protected static final CastleManager INSTANCE = new CastleManager();
    }
}