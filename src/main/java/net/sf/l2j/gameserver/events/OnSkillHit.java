package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.HashMap;
import java.util.Map;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnSkillHit {

    private final Map<String, Object> context = new HashMap<>();

    private Creature caster;
    private Creature target;
    private L2Skill skill;

    public <T extends L2Skill> T getSkill() {
        return (T) skill;
    }

    public OnSkillHit addContext(String parameter, Object value) {
        context.put(parameter, value);
        return this;
    }

    public OnSkillHit addContext(Map<String, Object> context) {
        this.context.putAll(context);
        return this;
    }

    public <T> T getContextValue(String key) {
        return (T) context.get(key);
    }

}
