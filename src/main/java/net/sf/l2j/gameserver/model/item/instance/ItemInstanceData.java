package net.sf.l2j.gameserver.model.item.instance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.model.Augmentation;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author finfan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public Item get() {
        return ItemData.getInstance().getTemplate(itemId);
    }

    public int getCrystalCount() {
        return get().getCrystalCount(enchantLevel);
    }

}
