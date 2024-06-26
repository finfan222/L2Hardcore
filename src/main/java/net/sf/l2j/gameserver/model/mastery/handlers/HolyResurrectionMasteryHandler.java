package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public class HolyResurrectionMasteryHandler implements MasteryHandler {

    private static final int SKILL_ID = 1016;

    @Override
    public void onLearn(Player player) {
        player.addSkill(SkillTable.getInstance().getInfo(SKILL_ID, 10), false);
    }

    @Override
    public void onUnlearn(Player player) {
        player.removeSkill(SKILL_ID, false);
    }

}
