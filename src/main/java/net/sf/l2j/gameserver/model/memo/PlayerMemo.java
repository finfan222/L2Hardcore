package net.sf.l2j.gameserver.model.memo;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.data.MemoSet;
import net.sf.l2j.commons.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * An implementation of {@link MemoSet} used for Player. There is a restore/save system.
 */
@Slf4j
public class PlayerMemo extends MemoSet {
    private static final long serialVersionUID = 1L;

    private static final String SELECT_MEMOS = "SELECT * FROM character_memo WHERE charId = ?";
    private static final String DELETE_MEMO = "DELETE FROM character_memo WHERE charId = ? AND var = ?";
    private static final String INSERT_OR_UPDATE_MEMO = "INSERT INTO character_memo (charId, var, val) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE val = VALUES(val)";

    private final int _objectId;

    public PlayerMemo(int objectId) {
        _objectId = objectId;

        // Restore memos.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_MEMOS)) {
            ps.setInt(1, _objectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    put(rs.getString("var"), rs.getString("val"));
                }
            }
        } catch (Exception e) {
            log.error("Couldn't restore memos for player id {}.", _objectId, e);
        }
    }

    @Override
    protected void onSet(String key, String value) {
        // Insert memo, on duplicate update it.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_OR_UPDATE_MEMO)) {
            ps.setInt(1, _objectId);
            ps.setString(2, key);
            ps.setString(3, value);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't set {} memo for player id {}.", key, _objectId, e);
        }
    }

    @Override
    protected void onUnset(String key) {
        // Clear memo.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_MEMO)) {
            ps.setInt(1, _objectId);
            ps.setString(2, key);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't unset {} memo for player id {}.", key, _objectId, e);
        }
    }
}