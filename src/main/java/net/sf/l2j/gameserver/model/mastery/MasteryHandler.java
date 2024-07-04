package net.sf.l2j.gameserver.model.mastery;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public interface MasteryHandler {

    void onLearn(Player player, MasteryData masteryData);

    void onUnlearn(Player player, MasteryData masteryData);

}
