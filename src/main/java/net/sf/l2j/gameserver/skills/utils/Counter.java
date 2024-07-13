package net.sf.l2j.gameserver.skills.utils;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.events.OnHit;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author finfan
 */
public class Counter {

    public enum Type {
        AFTER_EVADE,
        AFTER_BLOCK,
        AFTER_GET_CRIT,
        AFTER_PARRY;
    }

    private final Creature owner;
    private final Type type;
    private final int id;
    private final int level;
    private final int chance;

    private Counter(Creature owner, Type type, int id, int level, int chance) {
        this.owner = owner;
        this.type = type;
        this.id = id;
        this.level = level;
        this.chance = chance;
    }

    private void callAction(OnHit event) {
        Creature target = event.getTarget();
        if (target != owner || !Rnd.calcChance(chance, 100)) {
            return;
        }

        if (target.getCast().isCastingNow()) {
            return;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(id, level);
        if (skill.isMagic() && owner.isMuted()) {
            return;
        }

        if (skill.isPhysic() == owner.isPhysicalMuted()) {
            return;
        }

        switch (type) {
            case AFTER_BLOCK:
                if (event.getHit().block == ShieldDefense.FAILED) {
                    return;
                }
                break;

            case AFTER_GET_CRIT:
                if (!event.getHit().isCritical) {
                    return;
                }
                break;

            case AFTER_EVADE:
                if (!event.getHit().isMissed) {
                    return;
                }
                break;

            case AFTER_PARRY:
                //todo:
                break;
        }

        if (target instanceof Player player) {
            if (player.isAllSkillsDisabled()) {
                return;
            }

            player.getAI().tryToCast(event.getAttacker(), skill);
        } else {
            target.getCast().doCast(skill, event.getAttacker(), null);
        }

        owner.getEventListener().unsubscribe(this);
    }

    public static void start(Creature owner, Type type, int id, int level, int chance, OnHit event) {
        new Counter(owner, type, id, level, chance).callAction(event);
    }

}
