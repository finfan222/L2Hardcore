package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q272_WrathOfAncestors extends Quest {
    private static final String QUEST_NAME = "Q272_WrathOfAncestors";

    // Item
    private static final int GRAVE_ROBBERS_HEAD = 1474;

    public Q272_WrathOfAncestors() {
        super(272, "Wrath of Ancestors");

        setItemsIds(GRAVE_ROBBERS_HEAD);

        addStartNpc(30572); // Livina
        addTalkId(30572);

        addKillId(20319, 20320); // Goblin Grave Robber, Goblin Tomb Raider Leader
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 5;
        condition.races = new ClassRace[]{ClassRace.ORC};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30572-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        }

        return event;
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        String htmltext = getNoQuestMsg();
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        switch (st.getState()) {
            case CREATED:
                if (!condition.validateRace(player)) {
                    htmltext = "30572-00.htm";
                } else if (!condition.validateLevel(player)) {
                    htmltext = "30572-01.htm";
                } else {
                    htmltext = "30572-02.htm";
                }
                break;

            case STARTED:
                if (st.getCond() == 1) {
                    htmltext = "30572-04.htm";
                } else {
                    htmltext = "30572-05.htm";
                    takeItems(player, GRAVE_ROBBERS_HEAD, -1);
                    rewardItems(player, 57, 1500);
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

        if (dropItemsAlways(player, GRAVE_ROBBERS_HEAD, 1, 50)) {
            st.setCond(2);
        }

        return null;
    }
}