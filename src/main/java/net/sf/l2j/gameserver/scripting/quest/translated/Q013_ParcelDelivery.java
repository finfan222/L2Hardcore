package net.sf.l2j.gameserver.scripting.quest.translated;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q013_ParcelDelivery extends Quest {
    private static final String QUEST_NAME = "Q013_ParcelDelivery";

    // NPCs
    private static final int FUNDIN = 31274;
    private static final int VULCAN = 31539;

    // Item
    private static final int PACKAGE = 7263;

    public Q013_ParcelDelivery() {
        super(13, "Parcel Delivery");

        setItemsIds(PACKAGE);

        addStartNpc(FUNDIN);
        addTalkId(FUNDIN, VULCAN);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 74;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("31274-2.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
            giveItems(player, PACKAGE, 1);
        } else if (event.equalsIgnoreCase("31539-1.htm")) {
            takeItems(player, PACKAGE, 1);
            rewardItems(player, 57, 82656);
            playSound(player, SOUND_FINISH);
            st.exitQuest(false);
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
                htmltext = !condition.validateLevel(player) ? "31274-1.htm" : "31274-0.htm";
                break;

            case STARTED:
                switch (npc.getNpcId()) {
                    case FUNDIN:
                        htmltext = "31274-2.htm";
                        break;

                    case VULCAN:
                        htmltext = "31539-0.htm";
                        break;
                }
                break;

            case COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }

        return htmltext;
    }
}