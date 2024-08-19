package net.sf.l2j.gameserver.skills.basefuncs;

import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.conditions.Condition;

/**
 * @see Func
 */
public class FuncSet extends Func {
    public FuncSet(Object owner, Stats stat, double value, Condition cond) {
        super(owner, stat, 0, value, cond);
    }

    @Override
    public double calc(Creature effector, Creature effected, L2Skill skill, double base, double value) {
        // Condition does not exist or it fails, no change.
        if (getCond() != null && !getCond().test(effector, effected, skill)) {
            return value;
        }

        // Only for func set we use grip state
        if (effector instanceof Player player) {
            if (player.getTwoHandGrip().get() && player.getInventory().getItemFrom(Paperdoll.RHAND) != null) {
                switch (getStat()) {
                    // pAtk & mAtk is increased with two-handed grip
                    case POWER_ATTACK:
                    case MAGIC_ATTACK:
                        return Math.ceil(getValue() * 1.2170818505338078291814946619217);
                    // attack speed reduced with two-handed grip
                    case POWER_ATTACK_SPEED:
                        return getValue() - Math.ceil(getValue() * 0.16615384615384615384615384615385);
                }
            }
        }

        // Update value.
        return getValue();
    }
}