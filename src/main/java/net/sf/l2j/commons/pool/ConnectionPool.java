package net.sf.l2j.commons.pool;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public final class ConnectionPool {

    private static MariaDbPoolDataSource _source;

    public static void init() {
        try {
            _source = new MariaDbPoolDataSource();
            _source.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
            _source.setUrl(Config.DATABASE_URL);
            _source.setUser(Config.DATABASE_LOGIN);
            _source.setPassword(Config.DATABASE_PASSWORD);
            _source.setStaticGlobal(true);
        } catch (SQLException e) {
            log.error("Couldn't initialize connection pooler.", e);
        }
        log.info("Initializing ConnectionPool.");
    }

    public static void shutdown() {
        if (_source != null) {
            _source.close();
            _source = null;
        }
    }

    public static Connection getConnection() throws SQLException {
        return _source.getConnection();
    }
}