package net.sf.l2j.gameserver.model.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A container used by monster drops.<br>
 * <br>
 * The chance is exprimed as 1.000.000 to handle 4 point accuracy digits (100.0000%).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropData {

    public static final int MIN_CHANCE = 10000;
    public static final int MAX_CHANCE = 1000000;

    private int itemId;
    private int min;
    private int max;
    private int chance;

}