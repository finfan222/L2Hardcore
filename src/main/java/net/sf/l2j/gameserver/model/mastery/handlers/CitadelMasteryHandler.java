package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public class CitadelMasteryHandler implements MasteryHandler {

    public Boolean blockDamage(Player player, Creature attacker) {
        int pDef = player.getStatus().getPDef(attacker);
        int pAtk = attacker.getStatus().getPAtk(player);
        if (pDef <= pAtk) {
            return Boolean.FALSE;
        }

        int chance = (int) Math.round(pAtk * 1. / pDef * 100.);
        if (!Rnd.calcChance(chance, 100)) {
            return Boolean.FALSE;
        }

        player.sendMessage("Удар пришелся в часть брони. Урон значительно уменьшен.");
        return Boolean.TRUE;
    }

}
