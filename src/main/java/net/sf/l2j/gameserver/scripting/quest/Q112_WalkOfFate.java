package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q112_WalkOfFate extends Quest {
    private static final String QUEST_NAME = "Q112_WalkOfFate";

    // NPCs
    private static final int LIVINA = 30572;
    private static final int KARUDA = 32017;

    // Rewards
    private static final int ENCHANT_D = 956;

    public Q112_WalkOfFate() {
        super(112, "Walk of Fate");

        addStartNpc(LIVINA);
        addTalkId(LIVINA, KARUDA);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 20;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30572-02.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("32017-02.htm")) {
            giveItems(player, ENCHANT_D, 1);
            rewardItems(player, 57, 4665);
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
                htmltext = !condition.validateLevel(player) ? "30572-00.htm" : "30572-01.htm";
                break;

            case STARTED:
                htmltext = switch (npc.getNpcId()) {
                    case LIVINA -> "30572-03.htm";
                    case KARUDA -> "32017-01.htm";
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