package net.sf.l2j.gameserver.data.sql;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Bookmark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class loads and handles {@link Bookmark} into a List.<br> To retrieve a Bookmark, you need its name and the
 * player objectId.
 */
@Slf4j
public class BookmarkTable {

    private final List<Bookmark> _bks = new ArrayList<>();

    protected BookmarkTable() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM bookmarks");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                _bks.add(new Bookmark(rs.getString("name"), rs.getInt("obj_Id"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (Exception e) {
            log.error("Couldn't restore bookmarks.", e);
        }
        log.info("Loaded {} bookmarks.", _bks.size());
    }

    /**
     * Verify if a {@link Bookmark} already exists.
     *
     * @param name : The Bookmark name.
     * @param objId : The {@link Player} objectId to make checks on.
     * @return true if the Bookmark exists, false otherwise.
     */
    public boolean isExisting(String name, int objId) {
        return getBookmark(name, objId) != null;
    }

    /**
     * Retrieve a {@link Bookmark} by its name and its specific {@link Player} objectId.
     *
     * @param name : The Bookmark name.
     * @param objId : The Player objectId to make checks on.
     * @return the Bookmark if it exists, null otherwise.
     */
    public Bookmark getBookmark(String name, int objId) {
        for (Bookmark bk : _bks) {
            if (bk.getName().equalsIgnoreCase(name) && bk.getId() == objId) {
                return bk;
            }
        }
        return null;
    }

    /**
     * @param objId : The Player objectId to make checks on.
     * @return the list of {@link Bookmark}s of a {@link Player}.
     */
    public List<Bookmark> getBookmarks(int objId) {
        return _bks.stream().filter(bk -> bk.getId() == objId).collect(Collectors.toList());
    }

    /**
     * Creates a new {@link Bookmark} and store info to database.
     *
     * @param name : The name of the Bookmark.
     * @param player : The {@link Player} who requested the creation. We use the Player location as Bookmark location.
     */
    public void saveBookmark(String name, Player player) {
        final int objId = player.getObjectId();
        final int x = player.getX();
        final int y = player.getY();
        final int z = player.getZ();

        _bks.add(new Bookmark(name, objId, x, y, z));

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO bookmarks (name, obj_Id, x, y, z) values (?,?,?,?,?)")) {
            ps.setString(1, name);
            ps.setInt(2, objId);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't save bookmark.", e);
        }
    }

    /**
     * Delete a {@link Bookmark}, based on the {@link Player} objectId and its name.
     *
     * @param name : The name of the Bookmark.
     * @param objId : The Player objectId to make checks on.
     */
    public void deleteBookmark(String name, int objId) {
        final Bookmark bookmark = getBookmark(name, objId);
        if (bookmark != null) {
            _bks.remove(bookmark);

            try (Connection con = ConnectionPool.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM bookmarks WHERE name=? AND obj_Id=?")) {
                ps.setString(1, name);
                ps.setInt(2, objId);
                ps.execute();
            } catch (Exception e) {
                log.error("Couldn't delete bookmark.", e);
            }
        }
    }

    public static BookmarkTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final BookmarkTable INSTANCE = new BookmarkTable();
    }
}