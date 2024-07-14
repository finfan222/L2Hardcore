package net.sf.l2j.commons.pool;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public final class ConnectionPool {

    public static MariaDbPoolDataSource DATA_SOURCE;

    public static void init() {
        try {
            DATA_SOURCE = new MariaDbPoolDataSource();
            DATA_SOURCE.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
            DATA_SOURCE.setUrl(Config.DATABASE_URL);
            DATA_SOURCE.setUser(Config.DATABASE_LOGIN);
            DATA_SOURCE.setPassword(Config.DATABASE_PASSWORD);
            DATA_SOURCE.setStaticGlobal(true);
        } catch (SQLException e) {
            log.error("Couldn't initialize connection pooler.", e);
        }
        log.info("Initializing ConnectionPool.");
    }

    public static void shutdown() {
        if (DATA_SOURCE != null) {
            DATA_SOURCE.close();
            DATA_SOURCE = null;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }
}