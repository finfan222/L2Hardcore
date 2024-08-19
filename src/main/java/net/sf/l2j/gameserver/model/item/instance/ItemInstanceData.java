package net.sf.l2j.gameserver.model.item.instance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.data.xml.ItemManager;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemInstanceData {

    private int ownerId;
    private int dropperObjectId;
    private int count;
    private int itemId;
    private ItemLocation location;
    private int slot;
    private int enchantLevel;
    private int customType1;
    private int customType2;
    private boolean isDestroyProtected;
    private ItemState lastChange;
    private long time;
    private boolean isExistsInDB;

    public Item get() {
        return ItemManager.getInstance().getTemplate(itemId);
    }

    public int getCrystalCount() {
        return get().getCrystalCount(enchantLevel);
    }

}
