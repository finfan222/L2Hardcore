package net.sf.l2j.gameserver.model.cards;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.model.actor.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class CardsDao {

    public static void delete(int objectId, Connection con) throws SQLException {
        PreparedStatement st = con.prepareStatement("DELETE FROM character_cards WHERE objectId=?");
        st.setInt(1, objectId);
        st.executeUpdate();
        st.close();
        log.info("[CARDS]: All cards was removed for objectId={}.", objectId);
    }

    public static void update(CardEntity entity) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("REPLACE INTO character_cards (objectId,slotId,classIndex,symbolId) VALUES (?,?,?,?)")) {
            st.setInt(1, entity.getObjectId());
            st.setInt(2, entity.getSlotId());
            st.setInt(3, entity.getClassIndex());
            st.setInt(4, entity.getSymbolId());
            st.executeUpdate();
            log.info("[CARDS]: Entity {} was saved.", entity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(int objectId, int classIndex) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("DELETE FROM character_cards WHERE objectId=? AND classIndex=?")) {
            st.setInt(1, objectId);
            st.setInt(2, classIndex);
            st.executeUpdate();
            log.info("[CARDS]: All cards was removed for objectId={}/classIndex={}.", objectId, classIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(int objectId, int classIndex, int symbolId) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("DELETE FROM character_cards WHERE objectId=? AND classIndex=? AND symbolId=?")) {
            st.setInt(1, objectId);
            st.setInt(2, classIndex);
            st.setInt(3, symbolId);
            st.executeUpdate();
            log.info("[CARDS]: Card symbolId={} was removed from objectId={}/classIndex={}.", symbolId, objectId, classIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(int objectId) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("DELETE FROM character_cards WHERE objectId=?")) {
            st.setInt(1, objectId);
            st.executeUpdate();
            log.info("[CARDS]: All cards was removed for objectId={}.", objectId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void restore(Player player) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("SELECT symbolId, slotId FROM character_cards WHERE objectId=? AND classIndex=?")) {

            Cards cards = player.getCards();
            st.setInt(1, player.getObjectId());
            st.setInt(2, player.getClassIndex());
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    int symbolId = resultSet.getInt("symbolId");
                    int slotId = resultSet.getInt("slotId");
                    CardData data = CardManager.getInstance().get(symbolId);
                    cards.addCard(data, slotId);
                }
            }
            log.info("[CARDS]: All cards was restored for {}.", player);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
