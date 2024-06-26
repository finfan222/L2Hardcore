package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public interface MasteryHandler {

    default void onLearn(Player player) {}

    default void onUnlearn(Player player) {}

}
