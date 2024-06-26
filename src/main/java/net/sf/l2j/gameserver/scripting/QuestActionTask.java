package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public class QuestActionTask implements Runnable {

    protected final QuestState questState;
    protected final Player player;
    protected final AbnormalEffect abnormalEffect;

    public QuestActionTask(QuestState questState, AbnormalEffect abnormalEffect) {
        this.questState = questState;
        this.player = questState.getPlayer();
        this.abnormalEffect = abnormalEffect;
    }

    public void onStart() {
        player.setInvul(true);
        if (abnormalEffect != null) {
            player.startAbnormalEffect(abnormalEffect);
        }
    }

    public void onFinish() {
        player.setInvul(false);
        if (abnormalEffect != null) {
            player.stopAbnormalEffect(abnormalEffect);
        }
    }

    @Override
    public void run() {
    }

}
