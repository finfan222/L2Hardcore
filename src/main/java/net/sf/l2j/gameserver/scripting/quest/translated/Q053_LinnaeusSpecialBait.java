package net.sf.l2j.gameserver.scripting.quest.translated;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q053_LinnaeusSpecialBait extends Quest {
    private static final String QUEST_NAME = "Q053_LinnaeusSpecialBait";

    // Item
    private static final int CRIMSON_DRAKE_HEART = 7624;

    // Reward
    private static final int FLAMING_FISHING_LURE = 7613;

    public Q053_LinnaeusSpecialBait() {
        super(53, "Linnaues' Special Bait");

        setItemsIds(CRIMSON_DRAKE_HEART);

        addStartNpc(31577); // Linnaeus
        addTalkId(31577);

        addKillId(20670); // Crimson Drake
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 60;
        condition.items = new QuestDetail[]{QuestDetail.builder().id(CRIMSON_DRAKE_HEART).value(100).build()};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        if (event.equalsIgnoreCase("31577-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("31577-07.htm")) {
            htmltext = "31577-06.htm";
            takeItems(player, CRIMSON_DRAKE_HEART, -1);
            rewardItems(player, FLAMING_FISHING_LURE, 4);
            playSound(player, SOUND_FINISH);
            st.exitQuest(false);
        }

        return htmltext;
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        String htmltext = getNoQuestMsg();
        if (st == null) {
            return htmltext;
        }

        htmltext = switch (st.getState()) {
            case CREATED -> !condition.validateLevel(player) ? "31577-02.htm" : "31577-01.htm";
            case STARTED -> condition.validateItems(player) ? "31577-04.htm" : "31577-05.htm";
            case COMPLETED -> getAlreadyCompletedMsg();
        };

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = checkPlayerCondition(player, npc, 1);
        if (st == null) {
            return null;
        }

        if (dropItems(player, CRIMSON_DRAKE_HEART, 1, 100, 500000)) {
            st.setCond(2);
        }

        return null;
    }
}