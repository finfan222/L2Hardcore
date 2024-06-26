package net.sf.l2j.gameserver.model.mastery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.events.OnLevelChange;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.handlers.MasteryHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author finfan
 */
public class Mastery {

    private final Player player;

    private final MasteryType[] values = new MasteryType[7];
    private final List<MasteryTask> tasks = new ArrayList<>(10);
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private int nextMasteryIndex;
    private int points;

    public Mastery(Player player) {
        this.player = player;
        player.getEventListener().subscribe().cast(OnLevelChange.class).forEach(e -> {
            if (e.getNewLevel() - e.getOldLevel() < 0) {
                return;
            }

            boolean givePoints = e.getNewLevel() % 10 == 0;
            if (givePoints) {
                int allPts = points;
                for (MasteryType next : values) {
                    if (next != null) {
                        ++allPts;
                    }
                }

                int mustHaveToThisLevel = (player.getStatus().getLevel() - 10) / 10;
                if (allPts >= mustHaveToThisLevel) {
                    return;
                }

                synchronized (this) {
                    points++;
                }

                player.sendMessage("Вы получили 1 очко мастерства!");
            }
        });

        restore();
    }

    public synchronized void add(MasteryType type, boolean newOne) {
        values[nextMasteryIndex++] = type;
        type.getHandler().onLearn(player);
        points--;
        if (newOne) {
            create(type);
        }
    }

    @Deprecated
    private void replace(MasteryType from, MasteryType to) {
        int index = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }

            if (values[i] == from) {
                index = i;
                break;
            }
        }
        values[index] = to;
        update(from, to);
    }

    public synchronized void clear() {
        if (delete()) {
            for (MasteryType value : values) {
                if (value != null) {
                    value.getHandler().onUnlearn(player);
                }
            }
            Arrays.fill(values, null);
        }
    }

    public <T extends MasteryHandler> T get(MasteryType type) {
        for (MasteryType t : values) {
            if (t == type) {
                return (T) t.getHandler();
            }
        }
        throw new UnsupportedOperationException("Mastery X not exist on player " + player);
    }

    public boolean isHasMastery(MasteryType type) {
        if (nextMasteryIndex == 0) {
            return false;
        }

        return ArraysUtil.contains(values, type);
    }

    public boolean isCanLearn(Player player, MasteryType type) {
        if (player.getDialog() != null || player.isProcessingRequest() || player.isProcessingTransaction()) {
            player.sendMessage("Операция отклонена. Разберитесь со своими делами и попробуйте позже.");
            return false;
        }

        if (points < 1) {
            player.sendMessage("У вас недостаточно очков мастерства. Наберитесь опыта и попробуйте снова.");
            return false;
        }

        if (isHasMastery(type)) {
            throw new RuntimeException(String.format("Player %s trying to learn %s mastery which already have.", player, type));
        }

        if (type.getRequiredLevel() > player.getStatus().getLevel()) {
            player.sendMessage("Этот вид мастерства требует " + type.getRequiredLevel() + " (или выше) уровень.");
            return false;
        }

        return true;
    }

    private void restore() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery (object_id, points) VALUES (?,?) ON DUPLICATE KEY UPDATE object_id = object_id")) {
            st.setInt(1, player.getObjectId());
            st.setInt(2, points);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // create new one for entered player if not exists or do nothing (just set id to id)
            PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery (object_id, points) VALUES (?,?) ON DUPLICATE KEY UPDATE object_id = object_id");
            st.setInt(1, player.getObjectId());
            st.setInt(2, points);
            st.executeUpdate();

            st = con.prepareStatement("SELECT * FROM character_mastery_list WHERE object_id=?");
            st.setInt(1, player.getObjectId());
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    add(MasteryType.valueOf(resultSet.getString("mastery_type")), false);
                }
            }

            st = con.prepareStatement("SELECT * FROM character_mastery WHERE object_id=?");
            st.setInt(1, player.getObjectId());
            try (ResultSet resultSet = st.executeQuery()) {
                while (resultSet.next()) {
                    points = resultSet.getInt("points");
                }
            }
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void create(MasteryType masteryType) {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("INSERT INTO character_mastery_list(object_id, mastery_type) VALUES(?,?)");
            st.setInt(1, player.getObjectId());
            st.setString(2, masteryType.name());
            st.executeUpdate();

            st = con.prepareStatement("REPLACE INTO character_mastery (points, object_id) VALUES (?,?)");
            st.setInt(1, points);
            st.setInt(2, player.getObjectId());
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    private void update(MasteryType oldMasteryType, MasteryType newMasteryType) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement("UPDATE character_mastery_list SET mastery_type=? WHERE object_id=? AND mastery_type=?")) {
            st.setString(1, newMasteryType.name());
            st.setInt(2, player.getObjectId());
            st.setString(3, oldMasteryType.name());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean delete() {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("DELETE FROM character_mastery_list WHERE object_id=?");
            st.setInt(1, player.getObjectId());
            st.executeUpdate();

            st = con.prepareStatement("UPDATE character_mastery SET points=? WHERE object_id=?");
            st.setInt(1, Math.max((player.getStatus().getLevel() - 10) / 10, 0));
            st.setInt(2, player.getObjectId());
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addTask(MasteryTask task) {
        tasks.add(task);
    }

    public void removeTask(Object owner) {
        tasks.removeIf(e -> {
            if (e.owner == owner) {
                e.task.cancel(false);
                return true;
            }

            return false;
        });
    }

    public record MasteryTask(Object owner, ScheduledFuture<?> task) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variable<T> {

        private T value;

    }

    public <T> void setVariable(String name, T value) {
        variables.put(name, value);
    }

    public <T> T getVariable(String name) {
        return (T) variables.get(name);
    }

    public void clearVariables() {
        variables.clear();
    }

}
