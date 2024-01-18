package net.sf.l2j.gameserver.model.item.instance;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.item.instance.modules.DurabilityModule;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author finfan
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFactory {

    public static final Logger ITEM_LOG = Logger.getLogger("item");

    public static ItemInstance create(int itemId, int count, WorldObject creator, WorldObject reference) {
        ItemInstance item = new ItemInstance(IdFactory.getInstance().getNextId(), itemId);
        World.getInstance().addObject(item);
        if (item.isStackable() && count > 1) {
            item.setCount(count);
        }

        if (item.isWeapon() || item.isArmor()) {
            item.registerModule(new DurabilityModule(item));
        }

        if (Config.LOG_ITEMS) {
            final LogRecord record = new LogRecord(Level.INFO, "CREATE");
            record.setLoggerName("item");
            record.setParameters(new Object[]{
                creator,
                item,
                reference
            });
            ITEM_LOG.log(record);
        }

        return item;
    }

    public static ItemInstance createOnRestore(int objectId, int itemId, int count) {
        ItemInstance item = new ItemInstance(objectId, itemId);
        World.getInstance().addObject(item);
        if (item.isStackable() && count > 1) {
            item.setCount(count);
        }

        if (item.isWeapon() || item.isArmor()) {
            item.registerModule(new DurabilityModule(item));
        }

        if (Config.LOG_ITEMS) {
            final LogRecord record = new LogRecord(Level.INFO, "RESTORE");
            record.setLoggerName("item");
            record.setParameters(new Object[]{
                objectId,
                item,
            });
            ITEM_LOG.log(record);
        }

        return item;
    }
}
