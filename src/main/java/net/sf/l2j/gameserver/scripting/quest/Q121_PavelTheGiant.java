package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q121_PavelTheGiant extends Quest {
    private static final String QUEST_NAME = "Q121_PavelTheGiant";

    // NPCs
    private static final int NEWYEAR = 31961;
    private static final int YUMI = 32041;

    public Q121_PavelTheGiant() {
        super(121, "Pavel the Giant");

        addStartNpc(NEWYEAR);
        addTalkId(NEWYEAR, YUMI);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 46;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("31961-2.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("32041-2.htm")) {
            rewardExpAndSp(player, 10000, 0);
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
                htmltext = !condition.validateLevel(player) ? "31961-1a.htm" : "31961-1.htm";
                break;

            case STARTED:
                htmltext = switch (npc.getNpcId()) {
                    case NEWYEAR -> "31961-2a.htm";
                    case YUMI -> "32041-1.htm";
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