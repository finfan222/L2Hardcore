package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q647_InfluxOfMachines extends Quest {
    private static final String QUEST_NAME = "Q647_InfluxOfMachines";

    // Item
    private static final int DESTROYED_GOLEM_SHARD = 8100;

    // NPC
    private static final int GUTENHAGEN = 32069;

    public Q647_InfluxOfMachines() {
        super(647, "Influx of Machines");

        setItemsIds(DESTROYED_GOLEM_SHARD);

        addStartNpc(GUTENHAGEN);
        addTalkId(GUTENHAGEN);

        for (int i = 22052; i < 22079; i++) {
            addKillId(i);
        }
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        if (event.equalsIgnoreCase("32069-02.htm")) {
            st.setState(QuestStatus.STARTED);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("32069-06.htm")) {
            takeItems(player, DESTROYED_GOLEM_SHARD, -1);
            giveItems(player, Rnd.get(4963, 4972), 1);
            playSound(player, SOUND_FINISH);
            st.exitQuest(true);
        }

        return htmltext;
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
                htmltext = (player.getStatus().getLevel() < 46) ? "32069-03.htm" : "32069-01.htm";
                break;

            case STARTED:
                final int cond = st.getCond();
                if (cond == 1) {
                    htmltext = "32069-04.htm";
                } else if (cond == 2) {
                    htmltext = "32069-05.htm";
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = getRandomPartyMember(player, npc, 1);
        if (st == null) {
            return null;
        }

        if (dropItems(st.getPlayer(), DESTROYED_GOLEM_SHARD, 1, 500, 300000)) {
            st.setCond(2);
        }

        return null;
    }
}