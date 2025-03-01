package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q297_GatekeepersFavor extends Quest {
    private static final String QUEST_NAME = "Q297_GatekeepersFavor";

    // Item
    private static final int STARSTONE = 1573;

    // Reward
    private static final int GATEKEEPER_TOKEN = 1659;

    public Q297_GatekeepersFavor() {
        super(297, "Gatekeeper's Favor");

        setItemsIds(STARSTONE);

        addStartNpc(30540); // Wirphy
        addTalkId(30540);

        addKillId(20521); // Whinstone Golem
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 15;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30540-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        }

        return event;
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        String htmltext = getNoQuestMsg();
        if (st == null) {
            return htmltext;
        }

        switch (st.getState()) {
            case CREATED:
                htmltext = !condition.validateLevel(player) ? "30540-01.htm" : "30540-02.htm";
                break;

            case STARTED:
                if (st.getCond() == 1) {
                    htmltext = "30540-04.htm";
                } else {
                    htmltext = "30540-05.htm";
                    takeItems(player, STARSTONE, -1);
                    rewardItems(player, GATEKEEPER_TOKEN, 2);
                    playSound(player, SOUND_FINISH);
                    st.exitQuest(true);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = checkPlayerCondition(player, npc, 1);
        if (st == null) {
            return null;
        }

        if (dropItems(player, STARSTONE, 1, 20, 500000)) {
            st.setCond(2);
        }

        return null;
    }
}