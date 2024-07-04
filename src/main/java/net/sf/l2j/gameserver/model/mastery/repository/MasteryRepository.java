package net.sf.l2j.gameserver.model.mastery.repository;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.model.mastery.Mastery;
import net.sf.l2j.gameserver.model.mastery.MasteryManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author finfan
 */
@Slf4j
public class MasteryRepository {

    public void restore(Mastery mastery) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery (object_id, points) VALUES (?,?) ON DUPLICATE KEY UPDATE object_id = object_id")) {
            st.setInt(1, mastery.getPlayer().getObjectId());
            st.setInt(2, mastery.getPoints());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // create new one for entered player if not exists or do nothing (just set id to id)
            PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery (object_id, points) VALUES (?,?) ON DUPLICATE KEY UPDATE object_id = object_id");
            st.setInt(1, mastery.getPlayer().getObjectId());
            st.setInt(2, mastery.getPoints());
            st.executeUpdate();

            st = con.prepareStatement("SELECT * FROM character_mastery_list WHERE object_id=?");
            st.setInt(1, mastery.getPlayer().getObjectId());
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    int masteryId = resultSet.getInt("mastery_id");
                    mastery.addMastery(MasteryManager.getInstance().getById(masteryId));
                }
            }

            st = con.prepareStatement("SELECT * FROM character_mastery WHERE object_id=?");
            st.setInt(1, mastery.getPlayer().getObjectId());
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    mastery.setPoints(resultSet.getInt("points"));
                }
            }
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(Mastery mastery, int masteryId) {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery_list(object_id, mastery_id) VALUES(?,?)");
            st.setInt(1, mastery.getPlayer().getObjectId());
            st.setInt(2, masteryId);
            st.executeUpdate();

            st = con.prepareStatement("REPLACE INTO character_mastery (object_id, points) VALUES (?,?)");
            st.setInt(1, mastery.getPlayer().getObjectId());
            st.setInt(2, mastery.getPoints());
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(int objectId, int newPoints) {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("DELETE FROM character_mastery_list WHERE object_id=?");
            st.setInt(1, objectId);
            st.executeUpdate();

            st = con.prepareStatement("UPDATE character_mastery SET points=? WHERE object_id=?");
            st.setInt(1, newPoints);
            st.setInt(2, objectId);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void create() {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("""
                CREATE TABLE IF NOT EXISTS `character_mastery` (
                  `object_id` int(10) UNSIGNED NOT NULL,
                  `points` int(10) NULL DEFAULT NULL,
                  PRIMARY KEY (`object_id`) USING BTREE,
                  CONSTRAINT `fk_mastery_to_characters` FOREIGN KEY (`object_id`) REFERENCES `characters` (`obj_Id`) ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;
                """);
            st.executeUpdate();

            st = con.prepareStatement("""
                CREATE TABLE IF NOT EXISTS `character_mastery_list` (
                  `object_id` int(10) UNSIGNED NOT NULL,
                  `mastery_id` int(10) UNSIGNED NOT NULL,
                  PRIMARY KEY (`object_id`) USING BTREE,
                  CONSTRAINT `fk_mastery_list_to_characters` FOREIGN KEY (`object_id`) REFERENCES `characters` (`obj_Id`) ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;
                """);
            st.executeUpdate();
        } catch (SQLException e) {
            log.error("Error on creating mastery tables.", e);
        }
    }

}
