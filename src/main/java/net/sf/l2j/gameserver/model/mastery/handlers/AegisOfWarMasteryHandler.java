package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.events.OnAttacked;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.Mastery;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author finfan
 */
public class AegisOfWarMasteryHandler implements MasteryHandler {

    private static final int AEGIS_STANCE = 318;
    private static final int SKILL_ID = 4710;
    private static final String VAR_NAME = "blockCounter";

    @Override
    public void onLearn(Player player) {
        player.getMastery().setVariable(VAR_NAME, 0);
        player.getEventListener().subscribe().group(this).cast(OnAttacked.class).forEach(this::onAttacked);
    }

    @Override
    public void onUnlearn(Player player) {
        player.getEventListener().unsubscribe(this);
    }

    private void onAttacked(OnAttacked<Creature> event) {
        Creature attacker = event.getAttacker();
        Player defender = (Player) event.getTarget();
        if (defender.getFirstEffect(AEGIS_STANCE) != null) {
            Mastery mastery = event.getTarget().getMastery();
            if (event.getAttack().getType() != WeaponType.BOW) {
                int value = mastery.getVariable(VAR_NAME);
                if (value >= 20) {
                    defender.broadcastPacket(new MagicSkillUse(defender, attacker, 1,1,0,0));
                    defender.sendMessage("Сработало сокрушение эгиды!");
                    L2Skill info = SkillTable.getInstance().getInfo(SKILL_ID, 9);
                    info.applyEffects(defender, attacker);
                    mastery.setVariable(VAR_NAME, 0);
                } else {
                    mastery.setVariable(VAR_NAME, value + 1);
                }
            }
        }
    }
}
