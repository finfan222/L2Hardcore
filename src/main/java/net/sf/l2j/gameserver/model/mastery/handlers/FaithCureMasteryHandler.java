package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.events.OnHit;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.Mastery;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author finfan
 */
public class FaithCureMasteryHandler implements MasteryHandler {

    private static final String VAR_NAME = "receivedDamage";
    private static final int RECEIVE_DAMAGE = 834;

    @Override
    public void onLearn(Player player) {
        player.getEventListener().subscribe().group(this).cast(OnHit.class).forEach(this::onHit);
        MasteryHandler.super.onLearn(player);
    }

    @Override
    public void onUnlearn(Player player) {
        MasteryHandler.super.onUnlearn(player);
    }

    private void onHit(OnHit event) {
        Creature target = event.getTarget();
        Mastery mastery = target.getMastery();
        double receivedDamage = mastery.getVariable(VAR_NAME);
        mastery.setVariable(VAR_NAME, receivedDamage + event.getHit().damage);
        if (receivedDamage >= RECEIVE_DAMAGE) {
            L2Skill info = SkillTable.getInstance().getInfo(1, 1);
            info.applyEffects(target, target);
            target.broadcastPacket(new MagicSkillUse(target, target, info.getId(), 1, 0, 0));
            mastery.setVariable(VAR_NAME, 0);
        }
    }

}
