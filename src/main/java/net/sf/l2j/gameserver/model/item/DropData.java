package net.sf.l2j.gameserver.model.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.commons.random.Rnd;
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

    public static final int MIN_CHANCE = 1000;
    public static final int MAX_CHANCE = 1000000;

    private int itemId;
    private int min;
    private int max;
    private int chance;

    public DropData(int itemId, int min, int max, int chance) {
        this.itemId = itemId;
        this.min = min;
        this.max = max;
        this.chance = calculate(itemId, chance);
    }

    private int calculate(int itemId, int baseChance) {
        Item template = ItemData.getInstance().getTemplate(itemId);
        if (template == null) {
            throw new NullPointerException(String.format("Item %d is null.", itemId));
        }

        if (itemId == Item.ADENA) {
            return DropData.MAX_CHANCE;
        }

        if (baseChance == 0) {
            return 0;
        }

        double chance = getChance(baseChance, template);

        return (int) Math.round(chance);
    }

    private static double getChance(int baseChance, Item template) {
        int referencePrice = template.getReferencePrice();
        double chance = 0;
        if (referencePrice > 0) {
            double quad = 1. / 4.;
            chance = Math.pow(referencePrice, quad);
        }

        if (chance == 0) {
            chance = baseChance;
        } else {
            chance = DropData.MAX_CHANCE / chance;
        }

        chance /= baseChance / chance + 1.;
        return Math.min(Math.max(chance, DropData.MIN_CHANCE), DropData.MAX_CHANCE);
    }

    public String getFormattedChance() {
        return String.format("%.2f", (double) chance / MAX_CHANCE * 100.) + "%";
    }

    public int getRandomCount() {
        return Rnd.get(min, max);
    }
}