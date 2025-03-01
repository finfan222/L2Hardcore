package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q110_ToThePrimevalIsle extends Quest {
    private static final String QUEST_NAME = "Q110_ToThePrimevalIsle";

    // NPCs
    private static final int ANTON = 31338;
    private static final int MARQUEZ = 32113;

    // Item
    private static final int ANCIENT_BOOK = 8777;

    public Q110_ToThePrimevalIsle() {
        super(110, "To the Primeval Isle");

        setItemsIds(ANCIENT_BOOK);

        addStartNpc(ANTON);
        addTalkId(ANTON, MARQUEZ);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 75;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("31338-02.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
            giveItems(player, ANCIENT_BOOK, 1);
        } else if (event.equalsIgnoreCase("32113-03.htm") && player.getInventory().hasItems(ANCIENT_BOOK)) {
            takeItems(player, ANCIENT_BOOK, 1);
            rewardItems(player, 57, 169380);
            playSound(player, SOUND_FINISH);
            st.exitQuest(false);
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
                htmltext = !condition.validateLevel(player) ? "31338-00.htm" : "31338-01.htm";
                break;

            case STARTED:
                htmltext = switch (npc.getNpcId()) {
                    case ANTON -> "31338-01c.htm";
                    case MARQUEZ -> "32113-01.htm";
                    default -> htmltext;
                };
                break;

            case COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }

        return htmltext;
    }
}