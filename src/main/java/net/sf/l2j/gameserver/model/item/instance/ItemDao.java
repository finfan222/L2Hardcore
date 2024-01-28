package net.sf.l2j.gameserver.model.item.instance;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.model.Augmentation;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.item.instance.modules.DurabilityModule;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author finfan
 */
public class ItemDao {

    public static final CLogger LOGGER = new CLogger(WorldObject.class.getName());

    private static final String DELETE_AUGMENTATION = "DELETE FROM item_augmentations WHERE object_id = ?";
    private static final String RESTORE_AUGMENTATION = "SELECT attributes, skill_id, skill_level FROM item_augmentations WHERE object_id = ?";
    private static final String INSERT_AUGMENTATION = "INSERT INTO item_augmentations VALUES(?,?,?,?)";

    private static final String UPDATE_ITEM = """
        UPDATE items
        SET
         owner_id=?,
         count=?,
         location=?,
         slot=?,
         enchant_level=?,
         custom_type1=?,
         custom_type2=?,
         durability=?,
         time=?
        WHERE object_id=?
        """;

    private static final String INSERT_ITEM = """
        INSERT INTO items (
            owner_id,
            object_id,
            item_id,
            count,
            enchant_level,
            location,
            slot,
            custom_type1,
            custom_type2,
            durability,
            time)
        VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """;

    private static final String DELETE_ITEM = "DELETE FROM items WHERE object_id=?";
    private static final String DELETE_PET_ITEM = "DELETE FROM pets WHERE object_id=?";

    private static final ReentrantLock LOCKER = new ReentrantLock();

    /**
     * Remove the augmentation.
     */
    public static void removeAugmentation(ItemInstance item) {
        if (!item.isAugmented()) {
            return;
        }

        item.setAugmentation(null);

        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_AUGMENTATION)) {
            ps.setInt(1, item.getObjectId());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Couldn't remove augmentation for {}.", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }

    public static void restoreAttributes(ItemInstance item) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_AUGMENTATION)) {
            ps.setInt(1, item.getObjectId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.setAugmentation(
                        new Augmentation(rs.getInt("attributes"),
                            rs.getInt("skill_id"),
                            rs.getInt("skill_level"))
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't restore augmentation for {}.", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }

    public static void createAugmentation(ItemInstance item) {
        if (!item.isAugmented()) {
            return;
        }

        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_AUGMENTATION)) {

            Augmentation augmentation = item.getAugmentation();
            ps.setInt(1, item.getObjectId());
            ps.setInt(2, augmentation.getId());
            if (augmentation.getSkill() != null) {
                ps.setInt(3, augmentation.getSkill().getId());
                ps.setInt(4, augmentation.getSkill().getLevel());
            } else {
                ps.setInt(3, -1);
                ps.setInt(4, -1);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Couldn't update attributes for {}.", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }


    /**
     * @param ownerId : objectID of the owner.
     * @param resultSet : the ResultSet of the item.
     * @return a ItemInstance stored in database from its objectID
     */
    public static ItemInstance restore(int ownerId, ResultSet resultSet) {
        LOCKER.lock();
        try {
            final int objectId = resultSet.getInt("object_id");
            final int itemId = resultSet.getInt("item_id");
            final int count = resultSet.getInt("count");
            final ItemLocation location = ItemLocation.valueOf(resultSet.getString("location"));
            final int slot = resultSet.getInt("slot");
            final int enchant = resultSet.getInt("enchant_level");
            final int type1 = resultSet.getInt("custom_type1");
            final int type2 = resultSet.getInt("custom_type2");
            final int durability = resultSet.getInt("durability");
            final long time = resultSet.getLong("time");

            final Item template = ItemData.getInstance().getTemplate(itemId);
            if (template == null) {
                return null;
            }

            ItemInstance item = ItemFactory.createOnRestore(objectId, itemId, count);
            item.getData().setOwnerId(ownerId);
            item.setCount(count);
            item.setEnchantLevel(enchant);
            item.setCustomType1(type1);
            item.setCustomType2(type2);
            item.setLocation(location);
            item.setSlot(slot);
            item.getData().setTime(time);
            item.getData().setExistsInDB(true);
            Optional.ofNullable(item.getModule(DurabilityModule.class)).ifPresent(e -> e.setDurability(durability));

            // load augmentation
            if (item.isEquipable()) {
                restoreAttributes(item);
            }

            return item;
        } catch (Exception e) {
            LOGGER.error("Couldn't restore an item owned by {}.", e, ownerId);
            return null;
        } finally {
            LOCKER.unlock();
        }
    }


    /**
     * Update the database with values of the item
     */
    public static void update(ItemInstance item) {
        DurabilityModule durabilityModule = item.getModule(DurabilityModule.class);

        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_ITEM)) {
            ps.setInt(1, item.getData().getOwnerId());
            ps.setInt(2, item.getCount());
            ps.setString(3, item.getData().getLocation().name());
            ps.setInt(4, item.getSlot());
            ps.setInt(5, item.getData().getEnchantLevel());
            ps.setInt(6, item.getCustomType1());
            ps.setInt(7, item.getCustomType2());
            if (durabilityModule != null) {
                ps.setInt(8, durabilityModule.getDurability());
            } else {
                ps.setInt(8, -1);
            }
            ps.setLong(9, item.getData().getTime());
            ps.setInt(10, item.getObjectId());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Couldn't update {}. ", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * Insert the item in database
     */
    public static void create(ItemInstance item) {
        DurabilityModule durabilityModule = item.getModule(DurabilityModule.class);
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_ITEM)) {
            ps.setInt(1, item.getData().getOwnerId());
            ps.setInt(2, item.getObjectId());
            ps.setInt(3, item.getItemId());
            ps.setInt(4, item.getCount());
            ps.setInt(5, item.getData().getEnchantLevel());
            ps.setString(6, item.getData().getLocation().name());
            ps.setInt(7, item.getSlot());
            ps.setInt(8, item.getCustomType1());
            ps.setInt(9, item.getCustomType2());
            if (durabilityModule != null) {
                ps.setInt(10, durabilityModule.getDurability());
            } else {
                ps.setInt(10, -1);
            }
            ps.setLong(11, item.getData().getTime());
            ps.executeUpdate();
            if (item.isWeapon()) {
                createAugmentation(item);
            }
            item.getData().setExistsInDB(true);
        } catch (SQLException e) {
            LOGGER.error("Couldn't insert {}.", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * Delete item from database
     */
    public static void remove(int objectId) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(DELETE_ITEM)) {
                ps.setInt(1, objectId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't delete item with id={}.", e, objectId);
        } finally {
            LOCKER.unlock();
        }
    }

    public static void removePetItem(ItemInstance item) {
        // if it's a pet control item, delete the pet as well
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_PET_ITEM)) {
            ps.setInt(1, item.getObjectId());
            ps.execute();
        } catch (Exception e) {
            LOGGER.error("Couldn't delete pet item: {}.", e, item.toString());
        } finally {
            LOCKER.unlock();
        }
    }

    public static void updateCount(ItemInstance item) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE items SET count=? WHERE object_id=?");
            preparedStatement.setInt(1, item.getCount());
            preparedStatement.setInt(2, item.getObjectId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //todo: logger
        } finally {
            LOCKER.unlock();
        }
    }

    public static void updateDurability(ItemInstance item) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE items SET durability=? WHERE object_id=?");
            preparedStatement.setInt(1, item.getModule(DurabilityModule.class).getDurability());
            preparedStatement.setInt(2, item.getObjectId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //todo: logger
        } finally {
            LOCKER.unlock();
        }
    }

    public static void updateCustomType2(ItemInstance item) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE items SET custom_type2=? WHERE object_id=?");
            preparedStatement.setInt(1, item.getCustomType2());
            preparedStatement.setInt(2, item.getObjectId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //todo: logger
        } finally {
            LOCKER.unlock();
        }
    }

    public static void updateEnchantLevel(ItemInstance item) {
        LOCKER.lock();
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("UPDATE items SET enchant_level=? WHERE object_id=?");
            preparedStatement.setInt(1, item.getData().getEnchantLevel());
            preparedStatement.setInt(2, item.getObjectId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //todo: logger
        } finally {
            LOCKER.unlock();
        }
    }
}
