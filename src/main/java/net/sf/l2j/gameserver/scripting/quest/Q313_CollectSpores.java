package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q313_CollectSpores extends Quest {
    private static final String QUEST_NAME = "Q313_CollectSpores";

    // Item
    private static final int SPORE_SAC = 1118;

    public Q313_CollectSpores() {
        super(313, "Collect Spores");

        setItemsIds(SPORE_SAC);

        addStartNpc(30150); // Herbiel
        addTalkId(30150);

        addKillId(20509); // Spore Fungus
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 8;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30150-05.htm")) {
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
                htmltext = !condition.validateLevel(player) ? "30150-02.htm" : "30150-03.htm";
                break;

            case STARTED:
                if (st.getCond() == 1) {
                    htmltext = "30150-06.htm";
                } else {
                    htmltext = "30150-07.htm";
                    takeItems(player, SPORE_SAC, -1);
                    rewardItems(player, 57, 3500);
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

        if (dropItems(player, SPORE_SAC, 1, 10, 400000)) {
            st.setCond(2);
        }

        return null;
    }
}