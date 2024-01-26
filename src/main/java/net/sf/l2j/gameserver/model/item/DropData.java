package net.sf.l2j.gameserver.model.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * A container used by monster drops.<br>
 * <br>
 * The chance is exprimed as 1.000.000 to handle 4 point accuracy digits (100.0000%).
 */
@Data
@NoArgsConstructor
public class DropData {

    public static final int MIN_CHANCE = 10000;
    public static final int MAX_CHANCE = 1000000;

    private int itemId;
    private int min;
    private int max;
    private int chance;

    public DropData(int itemId, int min, int max, int chance) {
        this.itemId = itemId;
        this.min = min;
        this.max = max;
        if (itemId == Item.ADENA) {
            this.chance = MAX_CHANCE;
        } else if (chance == 0) {
            this.chance = chance;
        } else {
            int calc = ItemData.getInstance().getTemplate(itemId).getReferencePrice();
            calc = (int) Math.pow(calc, 1. / 4.);
            this.chance = Math.max(calc, MIN_CHANCE);
        }
    }
}