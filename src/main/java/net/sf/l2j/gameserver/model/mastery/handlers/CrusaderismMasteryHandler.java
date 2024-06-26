package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.events.OnLevelChange;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public class CrusaderismMasteryHandler implements MasteryHandler {

    private static final int SKILL_ID = 463;

    @Override
    public void onLearn(Player player) {
        int level = Math.max(player.getStatus().getLevel() - 40, 1);
        player.addSkill(SkillTable.getInstance().getInfo(SKILL_ID, level), false);
        player.getEventListener().subscribe().group(this).cast(OnLevelChange.class).forEach(this::onLevelChange);
    }

    @Override
    public void onUnlearn(Player player) {
        player.getEventListener().unsubscribe(this);
        player.removeSkill(SKILL_ID, false);
    }

    private void onLevelChange(OnLevelChange event) {
        int level = Math.max(event.getPlayer().getStatus().getLevel() - 40, 1);
        if (event.getPlayer().getSkillLevel(SKILL_ID) < level) {
            if (event.getOldLevel() < event.getNewLevel()) {
                event.getPlayer().addSkill(SkillTable.getInstance().getInfo(SKILL_ID, level), false);
            }
        }
    }
}
