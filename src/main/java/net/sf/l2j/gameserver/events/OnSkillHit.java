package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.handlers.Default;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnSkillHit {

    private Creature caster;
    private Creature target;
    private L2Skill skill;
    private Default.Context context;

    @SuppressWarnings("unchecked")
    public <T extends L2Skill> T getSkill() {
        return (T) skill;
    }

}
