package net.sf.l2j.gameserver.model.mastery;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.mastery.handlers.CitadelMasteryHandler;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author finfan
 */
public class MasteryUtil {

    public static Boolean CitadelMastery_invoke(Creature target, Creature attacker) {
        Mastery component = target.getMastery();
        if (component != null && component.isHasMastery(MasteryType.CITADEL)) {
            CitadelMasteryHandler mastery = component.get(MasteryType.CITADEL);
            return mastery.blockDamage(target.getActingPlayer(), attacker);
        }

        return Boolean.FALSE;
    }

    public static boolean ResistanceMastery_invoke(Creature target) {
        return target.getMastery().isHasMastery(MasteryType.RESISTANCE) && Rnd.calcChance(15, 100);
    }

    public static boolean LivingArmour_invoke(Creature defender, L2Skill skill) {
        return skill.getId() == 110 && defender.getMastery().isHasMastery(MasteryType.LIVING_ARMOUR);
    }

    public static boolean VeteranMastery_invoke(Player player, ItemInstance item) {
        if (item == null) {
            return false;
        }

        if (item.getItem() instanceof Armor armor) {
            if (armor.isAccessory() || armor.getItemType() != ArmorType.HEAVY) {
                return false;
            }
        }

        return player.getMastery().isHasMastery(MasteryType.VETERAN);
    }

    public static boolean HolyLight_invoke(Creature caster, Creature target, L2Skill skill) {
        return caster.getMastery().isHasMastery(MasteryType.HOLY_LIGHT) && Formulas.calcMCrit(caster, target, skill);
    }

    public static boolean HolyResurrectMastery_invoke(Creature caster) {
        return caster.getMastery().isHasMastery(MasteryType.HOLY_RESURRECT);
    }

}
