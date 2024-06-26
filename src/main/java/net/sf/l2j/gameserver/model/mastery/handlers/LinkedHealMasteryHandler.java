package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.events.OnSkillHit;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.status.CreatureStatus;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author finfan
 */
public class LinkedHealMasteryHandler implements MasteryHandler {

    @Override
    public void onLearn(Player player) {
        player.getEventListener().subscribe().group(this).cast(OnSkillHit.class).forEach(this::onHeal);
    }

    @Override
    public void onUnlearn(Player player) {
        player.getEventListener().unsubscribe(this);
    }

    private void onHeal(OnSkillHit event) {
        if (!event.getSkill().isHeal()) {
            return;
        }

        Creature healer = event.getCaster();
        CreatureStatus<? extends Creature> status = healer.getStatus();
        if (healer != event.getTarget() && status.getHp() < status.getMaxHp()) {
            double amount = event.getContext().getValue();
            healer.broadcastPacket(new MagicSkillUse(healer, healer,4527,1,0,0));
            healer.sendMessage("Мастерство Linked Heal повлияло на вас. Вы были исцелены эквивалентно исцелению цели.");
            status.addHp(amount);
        }
    }

}
