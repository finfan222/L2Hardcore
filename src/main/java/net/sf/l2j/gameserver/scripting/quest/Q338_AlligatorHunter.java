package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q338_AlligatorHunter extends Quest {
    private static final String QUEST_NAME = "Q338_AlligatorHunter";

    // Item
    private static final int ALLIGATOR_PELT = 4337;

    public Q338_AlligatorHunter() {
        super(338, "Alligator Hunter");

        setItemsIds(ALLIGATOR_PELT);

        addStartNpc(30892); // Enverun
        addTalkId(30892);

        addKillId(20135); // Alligator
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 40;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30892-02.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30892-05.htm")) {
            final int pelts = player.getInventory().getItemCount(ALLIGATOR_PELT);

            int reward = pelts * 60;
            if (pelts > 10) {
                reward += 3430;
            }

            takeItems(player, ALLIGATOR_PELT, -1);
            rewardItems(player, 57, reward);
        } else if (event.equalsIgnoreCase("30892-08.htm")) {
            playSound(player, SOUND_FINISH);
            st.exitQuest(true);
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

        htmltext = switch (st.getState()) {
            case CREATED -> !condition.validateLevel(player) ? "30892-00.htm" : "30892-01.htm";
            case STARTED -> (player.getInventory().hasItems(ALLIGATOR_PELT)) ? "30892-03.htm" : "30892-04.htm";
            default -> htmltext;
        };

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = checkPlayerState(player, npc, QuestStatus.STARTED);
        if (st == null) {
            return null;
        }

        dropItemsAlways(player, ALLIGATOR_PELT, 1, 0);

        return null;
    }
}