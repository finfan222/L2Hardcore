package net.sf.l2j.gameserver.data.xml;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.data.DocumentItem;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class loads and stores all {@link Item} templates.
 */
@Slf4j
public class ItemData {

    public static ItemInstance DUMMY;

    private Item[] templates;

    protected ItemData() {
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        final File dir = new File("./data/xml/items");

        final Map<Integer, Armor> armors = new HashMap<>();
        final Map<Integer, EtcItem> etcItems = new HashMap<>();
        final Map<Integer, Weapon> weapons = new HashMap<>();

        int highest = 0;
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            DocumentItem document = new DocumentItem(file);
            document.parse();

            for (Item item : document.getItemList()) {
                if (highest < item.getItemId()) {
                    highest = item.getItemId();
                }

                if (item instanceof EtcItem) {
                    etcItems.put(item.getItemId(), (EtcItem) item);
                } else if (item instanceof Armor) {
                    armors.put(item.getItemId(), (Armor) item);
                } else {
                    weapons.put(item.getItemId(), (Weapon) item);
                }
            }
        }

        // Feed an array with all items templates.
        templates = new Item[highest + 1];

        for (Armor item : armors.values()) {
            templates[item.getItemId()] = item;
        }

        for (Weapon item : weapons.values()) {
            templates[item.getItemId()] = item;
        }

        for (EtcItem item : etcItems.values()) {
            templates[item.getItemId()] = item;
        }

        DUMMY = new ItemInstance(0, getTemplate(9209));

        log.info("Loaded items.");
    }

    /**
     * @param id : the item id to check.
     * @return the {@link Item} corresponding to the item id.
     */
    public Item getTemplate(int id) {
        return (id >= templates.length) ? null : templates[id];
    }

    public static ItemData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final ItemData INSTANCE = new ItemData();
    }
}