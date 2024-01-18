package net.sf.l2j.gameserver.model.item.instance.modules;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author finfan
 */
public interface ItemModule {

    default void onRegister(ItemInstance item) {
    }

}
