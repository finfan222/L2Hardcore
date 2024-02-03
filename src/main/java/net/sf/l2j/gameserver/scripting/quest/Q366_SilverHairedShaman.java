package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.HashMap;
import java.util.Map;

public class Q366_SilverHairedShaman extends Quest {
    private static final String QUEST_NAME = "Q366_SilverHairedShaman";

    // NPC
    private static final int DIETER = 30111;

    // Item
    private static final int HAIR = 5874;

    // Drop chances
    private static final Map<Integer, Integer> CHANCES = new HashMap<>();

    static {
        CHANCES.put(20986, 560000);
        CHANCES.put(20987, 660000);
        CHANCES.put(20988, 620000);
    }

    public Q366_SilverHairedShaman() {
        super(366, "Silver Haired Shaman");

        setItemsIds(HAIR);

        addStartNpc(DIETER);
        addTalkId(DIETER);

        addKillId(20986, 20987, 20988);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 48;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30111-2.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30111-6.htm")) {
            playSound(player, SOUND_FINISH);
            st.exitQuest(true);
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
                htmltext = !condition.validateLevel(player) ? "30111-0.htm" : "30111-1.htm";
                break;

            case STARTED:
                final int count = player.getInventory().getItemCount(HAIR);
                if (count == 0) {
                    htmltext = "30111-3.htm";
                } else {
                    htmltext = "30111-4.htm";
                    takeItems(player, HAIR, -1);
                    rewardItems(player, 57, 12070 + 500 * count);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = getRandomPartyMemberState(player, npc, QuestStatus.STARTED);
        if (st == null) {
            return null;
        }

        dropItems(st.getPlayer(), HAIR, 1, 0, CHANCES.get(npc.getNpcId()));

        return null;
    }
}