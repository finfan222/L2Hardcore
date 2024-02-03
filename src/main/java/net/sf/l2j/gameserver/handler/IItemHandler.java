package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mother class of all itemHandlers.
 */
public interface IItemHandler {
    Logger log = LoggerFactory.getLogger(IItemHandler.class.getName());

    /**
     * Launch task associated to the item.
     *
     * @param playable L2Playable designating the player
     * @param item ItemInstance designating the item to use
     * @param forceUse ctrl hold on item use
     */
    void useItem(Playable playable, ItemInstance item, boolean forceUse);
}