package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q608_SlayTheEnemyCommander extends Quest {
    private static final String QUEST_NAME = "Q608_SlayTheEnemyCommander";

    // Quest Items
    private static final int HEAD_OF_MOS = 7236;
    private static final int TOTEM_OF_WISDOM = 7220;
    private static final int KETRA_ALLIANCE_4 = 7214;

    public Q608_SlayTheEnemyCommander() {
        super(608, "Slay the enemy commander!");

        setItemsIds(HEAD_OF_MOS);

        addStartNpc(31370); // Kadun Zu Ketra
        addTalkId(31370);

        addKillId(25312); // Mos
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        if (event.equalsIgnoreCase("31370-04.htm")) {
            st.setState(QuestStatus.STARTED);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("31370-07.htm")) {
            if (player.getInventory().hasItems(HEAD_OF_MOS)) {
                takeItems(player, HEAD_OF_MOS, -1);
                giveItems(player, TOTEM_OF_WISDOM, 1);
                rewardExpAndSp(player, 10000, 0);
                playSound(player, SOUND_FINISH);
                st.exitQuest(true);
            } else {
                htmltext = "31370-06.htm";
                st.setCond(1);
                playSound(player, SOUND_ACCEPT);
            }
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
                if (player.getStatus().getLevel() >= 75) {
                    if (player.getAllianceWithVarkaKetra() >= 4 && player.getInventory().hasItems(KETRA_ALLIANCE_4) && !player.getInventory().hasItems(TOTEM_OF_WISDOM)) {
                        htmltext = "31370-01.htm";
                    } else {
                        htmltext = "31370-02.htm";
                    }
                } else {
                    htmltext = "31370-03.htm";
                }
                break;

            case STARTED:
                htmltext = (player.getInventory().hasItems(HEAD_OF_MOS)) ? "31370-05.htm" : "31370-06.htm";
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        for (QuestState st : getPartyMembers(player, npc, 1)) {
            Player pm = st.getPlayer();
            if (pm.getAllianceWithVarkaKetra() >= 4 && pm.getInventory().hasItems(KETRA_ALLIANCE_4)) {
                st.setCond(2);
                playSound(pm, SOUND_MIDDLE);
                giveItems(pm, HEAD_OF_MOS, 1);
            }
        }

        return null;
    }
}