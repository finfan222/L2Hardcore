package net.sf.l2j.gameserver.data.xml;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.data.DocumentItem;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class loads and stores all {@link Item} templates.
 */
@Slf4j
public class ItemManager {

    @Getter(lazy = true)
    private static final ItemManager instance = new ItemManager();

    public static ItemInstance DUMMY;

    private final Map<Integer, Item> items = new HashMap<>();

    private ItemManager() {
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        final File dir = new File("./data/xml/items");

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            DocumentItem document = new DocumentItem(file);
            document.parse();
            document.getItemList().forEach(item -> items.put(item.getItemId(), item));
        }

        log.info("Loaded items {}", items.size());
        createDummyItem();
    }

    private void createDummyItem() {
        DUMMY = new ItemInstance(0, items.get(9209));
    }

    /**
     * @param id : the item id to check.
     * @return the {@link Item} corresponding to the item id.
     */
    public Item getTemplate(int id) {
        return items.get(id);
    }

}