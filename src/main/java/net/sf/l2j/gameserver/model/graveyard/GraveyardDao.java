package net.sf.l2j.gameserver.model.graveyard;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.sf.l2j.Config;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.Tombstone;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraveyardDao {

    private static final CLogger LOGGER = new CLogger(GraveyardDao.class.getSimpleName());

    public static void create(PostScript ps) {
        LOGGER.info("[9][GraveyardDao.create] {}", ps);
        PlayerInfoTable.getInstance().addRestrictedName(ps.getName());
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("INSERT INTO graveyard (name,message,reason,x,y,z,heading,date,is_eternal) VALUES (?,?,?,?,?,?,?,?,?)");
            st.setString(1, ps.getName());
            st.setString(2, ps.getMessage());
            st.setString(3, ps.getReason().name());
            st.setInt(4, ps.getX());
            st.setInt(5, ps.getY());
            st.setInt(6, ps.getZ());
            st.setInt(7, ps.getHeading());
            st.setDate(8, Date.valueOf(ps.getDate()));
            st.setBoolean(9, ps.isEternal());
            st.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't create tombstone with data={}.\n{}", ps, e);
        }

        createTombstoneNpc(ps);
    }

    public static void restore() {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("SELECT * FROM graveyard");
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString(1);
                    String message = resultSet.getString(2);
                    DieReason reason = DieReason.valueOf(resultSet.getString(3));
                    int x = resultSet.getInt(4);
                    int y = resultSet.getInt(5);
                    int z = resultSet.getInt(6);
                    int heading = resultSet.getInt(7);
                    LocalDate date = new Date(resultSet.getDate(8).getTime()).toLocalDate();
                    boolean isEternal = resultSet.getBoolean(9);

                    PostScript ps = PostScript.builder()
                        .name(name)
                        .message(message)
                        .reason(reason)
                        .x(x)
                        .y(y)
                        .z(z)
                        .heading(heading)
                        .date(date)
                        .isEternal(isEternal)
                        .build();

                    Tombstone tombstone = createTombstoneNpc(ps);
                    PlayerInfoTable.getInstance().addRestrictedName(ps.getName());
                    LOGGER.info("Tombstone {} was spawned.", tombstone);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Can't restore and spawn tombstones.", e);
        }
    }

    public static void delete() {
        String query = String.format("DELETE FROM graveyard WHERE ADDDATE(date, %d) <= CURRENT_DATE",
            TimeUnit.MILLISECONDS.toDays(Config.HARDCORE_TOMBSTONE_LIFETIME));
        try (Connection con = ConnectionPool.getConnection()) {
            con.prepareStatement(query).executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't delete graveyard.", e);
        }
    }

    private static Tombstone createTombstoneNpc(PostScript data) {
        NpcTemplate template = NpcData.getInstance().getTemplate(GraveyardManager.TOMBSTONE_ID);
        Tombstone tombstone = new Tombstone(IdFactory.getInstance().getNextId(), template, data);
        tombstone.getStatus().setMaxHpMp();
        tombstone.setInvul(true);
        SpawnLocation spawnLoc = new SpawnLocation(data.getX(), data.getY(), data.getZ(), data.getHeading());
        try {
            tombstone.spawnMe(spawnLoc);
        } catch (Exception e) {
            LOGGER.error("{}", e);
        }
        return tombstone;
    }

}
