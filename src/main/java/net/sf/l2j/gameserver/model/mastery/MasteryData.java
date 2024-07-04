package net.sf.l2j.gameserver.model.mastery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageColor;
import net.sf.l2j.gameserver.skills.L2Skill;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * @author finfan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasteryData {

    private int id;
    private String name;
    private String shortDescr; // unlearned
    private String fullDescr; // learned
    private int branchLevel;
    private ClassId[] classCond;
    private int levelCond;
    private SkillData[] skills;
    private MasteryHandler handler;
    private IconData icon;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillData {
        private int id;
        private int level;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IconData {
        private String learned;
        private String unlearned;
    }

    /**
     * Проверяет игрока на валидность его уровня и класса.
     *
     * @param player изучающий мастерство (кого проверяем)
     * @return {@code true} если пройдены проверки изучения {@code level, classId}, {@code false} в противном случае
     */
    public boolean checkRequirements(Player player) {
        ClassId classId = player.getClassId();
        int level = player.getStatus().getLevel();

        if (!ArrayUtils.contains(classCond, classId)) {
            player.sendMessage("", SystemMessageColor.RED_LIGHT);
            return false;
        }

        return levelCond == 0 ? level >= branchLevel * 10 + 10 : level >= levelCond;
    }

    /**
     * @return возвращает массив скиллов которые выдаются персонажу при изучении мастерства
     */
    public L2Skill[] getSkills() {
        return Arrays.stream(skills).map(e -> SkillTable.getInstance().getInfo(e.id, e.level)).toArray(L2Skill[]::new);
    }

    /**
     * @return возвращает актуальный уровень ветви где находится мастерство {@code branchLevel * 10 + 10}
     */
    public int getBranchLevel() {
        return branchLevel * 10 + 10;
    }

}
