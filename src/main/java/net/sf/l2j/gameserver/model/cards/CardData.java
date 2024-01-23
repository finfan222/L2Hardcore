package net.sf.l2j.gameserver.model.cards;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.skills.L2Skill;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardData {

    private String name;
    private int symbolId;
    private int itemId;
    private int bonusSTR;
    private int bonusDEX;
    private int bonusCON;
    private int bonusINT;
    private int bonusMEN;
    private int bonusWIT;
    private int bonusSkillId;

    public L2Skill getSkill() {
        return SkillTable.getInstance().getInfo(bonusSkillId, 1);
    }
}
