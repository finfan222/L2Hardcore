package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnSkillHitBy {

    private final Map<String, Serializable> context = new HashMap<>();

    private Creature caster;
    private Creature target;
    private L2Skill skill;

    @SuppressWarnings("unchecked")
    public <T extends L2Skill> T getSkill() {
        return (T) skill;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getContextValue(String key) {
        T value = (T) context.get(key);
        if (value == null) {
            throw new NullPointerException(String.format("Context is null because key %s not exist in map.", key));
        }

        return value;
    }
}
