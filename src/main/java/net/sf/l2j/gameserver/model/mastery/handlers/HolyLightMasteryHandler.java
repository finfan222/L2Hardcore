package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Увеличивает healPower на 50%
 * Позволяет иметь критический эффект исцелением зависящий от calcMCrit
 * Снижает потребление MP на HolyBlessing/DivineHeal на 20% и 40%
 *
 * @author finfan
 */
public class HolyLightMasteryHandler implements MasteryHandler {

    private static final int SKILL_ID = 1;

    @Override
    public void onLearn(Player player) {
        player.addSkill(SkillTable.getInstance().getInfo(SKILL_ID,1), false);
    }

    @Override
    public void onUnlearn(Player player) {
        player.removeSkill(SKILL_ID, false);
    }
}
