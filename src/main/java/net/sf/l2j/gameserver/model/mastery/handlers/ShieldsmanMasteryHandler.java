package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Добавляет навык {@code Perfect Block} в способности персонажа, позволяя включать его против следующей атаки врага.
 *
 * @author finfan
 */
public class ShieldsmanMasteryHandler implements MasteryHandler {

    private static final int SKILL_ID = 1; // fixme: ид скила адекватный

    @Override
    public void onLearn(Player player) {
        player.addSkill(SkillTable.getInstance().getInfo(SKILL_ID,1), false);
    }

    @Override
    public void onUnlearn(Player player) {
        player.removeSkill(SKILL_ID, false);
    }

}
