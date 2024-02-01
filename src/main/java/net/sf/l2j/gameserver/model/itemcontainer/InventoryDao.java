package net.sf.l2j.gameserver.model.itemcontainer;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemDao;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author finfan
 */
@Slf4j
public class InventoryDao {

    private static final String RESTORE_INVENTORY = "SELECT object_id, item_id, count, enchant_level, location, slot, custom_type1, custom_type2, durability, time FROM items WHERE owner_id=? AND (location=? OR location=?) ORDER BY slot";
    private static final String RESTORE_ITEM_CONTAINER = "SELECT object_id, item_id, count, enchant_level, location, slot, custom_type1, custom_type2, durability, time FROM items WHERE owner_id=? AND location=?";

    public static void restore(Inventory inventory) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_INVENTORY)) {
            ps.setInt(1, inventory.getOwnerId());
            ps.setString(2, inventory.getBaseLocation().name());
            ps.setString(3, inventory.getEquipLocation().name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Restore the item.
                    final ItemInstance item = ItemDao.restore(inventory.getOwnerId(), rs);
                    if (item == null) {
                        continue;
                    }

                    // If the item is an hero item and inventory's owner is a player who isn't an hero, then set it to inventory.
                    if (inventory.getOwner() instanceof Player && item.isHeroItem() && !HeroManager.getInstance().isActiveHero(inventory.getOwnerId())) {
                        item.setLocation(ItemLocation.INVENTORY);
                    }

                    // Add the item to world objects list.
                    World.getInstance().addObject(item);

                    // If stackable item is found in inventory just add to current quantity
                    if (item.isStackable() && inventory.getItemByItemId(item.getItemId()) != null) {
                        inventory.addItem("Restore", item, inventory.getOwner().getActingPlayer(), null);
                    } else {
                        inventory.addItem(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Couldn't restore inventory for {}.", inventory.getOwnerId(), e);
        }
    }

    public static void restore(ItemContainer container) {
        final Player owner = (container.getOwner() == null) ? null : container.getOwner().getActingPlayer();
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_ITEM_CONTAINER)) {
            ps.setInt(1, container.getOwnerId());
            ps.setString(2, container.getBaseLocation().name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Restore the item.
                    ItemInstance item = ItemDao.restore(container.getOwnerId(), rs);
                    if (item == null) {
                        continue;
                    }

                    // Add the item to world objects list.
                    World.getInstance().addObject(item);

                    // If stackable item is found in inventory just add to current quantity
                    if (item.isStackable() && container.getItemByItemId(item.getItemId()) != null) {
                        container.addItem("Restore", item, owner, null);
                    } else {
                        container.addItem(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Couldn't restore container for {}.", container.getOwnerId(), e);
        }
    }

}
