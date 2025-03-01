package net.sf.l2j.gameserver.data.sql;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.data.MemoSet;
import net.sf.l2j.commons.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A global, server-size, container for variables of any type, which can be then saved/restored upon server restart. It
 * extends {@link MemoSet}.
 */
@Slf4j
public class ServerMemoTable extends MemoSet {
    private static final long serialVersionUID = 1L;

    private static final String SELECT_MEMOS = "SELECT * FROM server_memo";
    private static final String DELETE_MEMO = "DELETE FROM server_memo WHERE var = ?";
    private static final String INSERT_OR_UPDATE_MEMO = "INSERT INTO server_memo (var, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)";

    protected ServerMemoTable() {
        // Restore previous variables.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_MEMOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                put(rs.getString("var"), rs.getString("value"));
            }
        } catch (Exception e) {
            log.error("Couldn't restore server variables.", e);
        }
        log.info("Loaded {} server variables.", size());
    }

    @Override
    protected void onSet(String key, String value) {
        // Insert memo, on duplicate update it.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_OR_UPDATE_MEMO)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't set {} server memo.", key, e);
        }
    }

    @Override
    protected void onUnset(String key) {
        // Clear memo.
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_MEMO)) {
            ps.setString(1, key);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't unset {} server memo.", key, e);
        }
    }

    public static final ServerMemoTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final ServerMemoTable INSTANCE = new ServerMemoTable();
    }
}