package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.HashMap;
import java.util.Map;

public class Q377_ExplorationOfTheGiantsCave_Part2 extends Quest {
    private static final String QUEST_NAME = "Q377_ExplorationOfTheGiantsCave_Part2";

    // Items
    private static final int ANCIENT_TITAN_BOOK = 5955;
    private static final int ANCIENT_DICTIONARY_INTERMEDIATE_LEVEL = 5892;

    private static final int[][] BOOKS =
        {
            // science & technology -> majestic leather, leather armor of nightmare
            {
                5945,
                5946,
                5947,
                5948,
                5949
            },
            // culture -> armor of nightmare, majestic plate
            {
                5950,
                5951,
                5952,
                5953,
                5954
            }
        };

    // Rewards
    private static final int[][] RECIPES =
        {
            // science & technology -> majestic leather, leather armor of nightmare
            {
                5338,
                5336
            },
            // culture -> armor of nightmare, majestic plate
            {
                5420,
                5422
            }
        };

    // Drop chances
    private static final Map<Integer, Integer> CHANCES = new HashMap<>();

    static {
        CHANCES.put(20654, 25000);
        CHANCES.put(20656, 22000);
        CHANCES.put(20657, 16000);
        CHANCES.put(20658, 15000);
    }

    public Q377_ExplorationOfTheGiantsCave_Part2() {
        super(377, "Exploration of the Giants' Cave, Part 2");

        addStartNpc(31147); // Sobling
        addTalkId(31147);

        for (int npcId : CHANCES.keySet()) {
            addKillId(npcId);
        }
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 57;
        condition.items = new QuestDetail[]{QuestDetail.builder().id(ANCIENT_DICTIONARY_INTERMEDIATE_LEVEL).build()};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        if (event.equalsIgnoreCase("31147-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("31147-04.htm")) {
            htmltext = checkItems(player);
        } else if (event.equalsIgnoreCase("31147-07.htm")) {
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

        htmltext = switch (st.getState()) {
            case CREATED -> (!condition.validateLevel(player) || !condition.validateItems(player)) ? "31147-01.htm" : "31147-02.htm";
            case STARTED -> checkItems(player);
            default -> htmltext;
        };

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = getRandomPartyMemberState(player, npc, QuestStatus.STARTED);
        if (st == null) {
            return null;
        }

        dropItems(st.getPlayer(), ANCIENT_TITAN_BOOK, 1, 0, CHANCES.get(npc.getNpcId()));

        return null;
    }

    private static String checkItems(Player player) {
        for (int type = 0; type < BOOKS.length; type++) {
            boolean complete = true;
            for (int book : BOOKS[type]) {
                if (!player.getInventory().hasItems(book)) {
                    complete = false;
                }
            }

            if (complete) {
                for (int book : BOOKS[type]) {
                    takeItems(player, book, 1);
                }

                giveItems(player, Rnd.get(RECIPES[type]), 1);
                return "31147-04.htm";
            }
        }
        return "31147-05.htm";
    }
}