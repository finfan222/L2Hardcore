package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q372_LegacyOfInsolence extends Quest {
    private static final String QUEST_NAME = "Q372_LegacyOfInsolence";

    // NPCs
    private static final int WALDERAL = 30844;
    private static final int PATRIN = 30929;
    private static final int HOLLY = 30839;
    private static final int CLAUDIA = 31001;
    private static final int DESMOND = 30855;

    // Monsters
    private static final int[][] MONSTERS_DROPS =
        {
            // npcId
            {
                20817,
                20821,
                20825,
                20829,
                21069,
                21063
            },
            // parchment (red, blue, black, white)
            {
                5966,
                5966,
                5966,
                5967,
                5968,
                5969
            },
            // rate
            {
                300000,
                400000,
                460000,
                400000,
                250000,
                250000
            }
        };

    // Items
    private static final int[][] SCROLLS =
        {
            // Walderal => 13 blueprints => parts, recipes.
            {
                5989,
                6001
            },
            // Holly -> 5x Imperial Genealogy -> Dark Crystal parts/Adena
            {
                5984,
                5988
            },
            // Patrin -> 5x Ancient Epic -> Tallum parts/Adena
            {
                5979,
                5983
            },
            // Claudia -> 7x Revelation of the Seals -> Nightmare parts/Adena
            {
                5972,
                5978
            },
            // Desmond -> 7x Revelation of the Seals -> Majestic parts/Adena
            {
                5972,
                5978
            }
        };

    // Rewards matrice.
    private static final int[][][] REWARDS_MATRICE =
        {
            // Walderal DC choice
            {
                {
                    13,
                    5496
                },
                {
                    26,
                    5508
                },
                {
                    40,
                    5525
                },
                {
                    58,
                    5368
                },
                {
                    76,
                    5392
                },
                {
                    100,
                    5426
                }
            },
            // Walderal Tallum choice
            {
                {
                    13,
                    5497
                },
                {
                    26,
                    5509
                },
                {
                    40,
                    5526
                },
                {
                    58,
                    5370
                },
                {
                    76,
                    5394
                },
                {
                    100,
                    5428
                }
            },
            // Walderal NM choice
            {
                {
                    20,
                    5502
                },
                {
                    40,
                    5514
                },
                {
                    58,
                    5527
                },
                {
                    73,
                    5380
                },
                {
                    87,
                    5404
                },
                {
                    100,
                    5430
                }
            },
            // Walderal Maja choice
            {
                {
                    20,
                    5503
                },
                {
                    40,
                    5515
                },
                {
                    58,
                    5528
                },
                {
                    73,
                    5382
                },
                {
                    87,
                    5406
                },
                {
                    100,
                    5432
                }
            },
            // Holly DC parts + adenas.
            {
                {
                    33,
                    5496
                },
                {
                    66,
                    5508
                },
                {
                    89,
                    5525
                },
                {
                    100,
                    57
                }
            },
            // Patrin Tallum parts + adenas.
            {
                {
                    33,
                    5497
                },
                {
                    66,
                    5509
                },
                {
                    89,
                    5526
                },
                {
                    100,
                    57
                }
            },
            // Claudia NM parts + adenas.
            {
                {
                    35,
                    5502
                },
                {
                    70,
                    5514
                },
                {
                    87,
                    5527
                },
                {
                    100,
                    57
                }
            },
            // Desmond Maja choice
            {
                {
                    35,
                    5503
                },
                {
                    70,
                    5515
                },
                {
                    87,
                    5528
                },
                {
                    100,
                    57
                }
            }
        };

    public Q372_LegacyOfInsolence() {
        super(372, "Legacy of Insolence");

        addStartNpc(WALDERAL);
        addTalkId(WALDERAL, PATRIN, HOLLY, CLAUDIA, DESMOND);

        addKillId(MONSTERS_DROPS[0]);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 59;
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        if (event.equalsIgnoreCase("30844-04.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30844-05b.htm")) {
            if (st.getCond() == 1) {
                st.setCond(2);
                playSound(player, SOUND_MIDDLE);
            }
        } else if (event.equalsIgnoreCase("30844-07.htm")) {
            for (int blueprint = 5989; blueprint <= 6001; blueprint++) {
                if (!player.getInventory().hasItems(blueprint)) {
                    htmltext = "30844-06.htm";
                    break;
                }
            }
        } else if (event.startsWith("30844-07-")) {
            checkAndRewardItems(player, 0, Integer.parseInt(event.substring(9, 10)), WALDERAL);
        } else if (event.equalsIgnoreCase("30844-09.htm")) {
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
                htmltext = !condition.validateLevel(player) ? "30844-01.htm" : "30844-02.htm";
                break;

            case STARTED:
                htmltext = switch (npc.getNpcId()) {
                    case WALDERAL -> "30844-05.htm";
                    case HOLLY -> checkAndRewardItems(player, 1, 4, HOLLY);
                    case PATRIN -> checkAndRewardItems(player, 2, 5, PATRIN);
                    case CLAUDIA -> checkAndRewardItems(player, 3, 6, CLAUDIA);
                    case DESMOND -> checkAndRewardItems(player, 4, 7, DESMOND);
                    default -> htmltext;
                };
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

        final int npcId = npc.getNpcId();

        for (int i = 0; i < MONSTERS_DROPS[0].length; i++) {
            if (MONSTERS_DROPS[0][i] == npcId) {
                dropItems(st.getPlayer(), MONSTERS_DROPS[1][i], 1, 0, MONSTERS_DROPS[2][i]);
                break;
            }
        }
        return null;
    }

    private static String checkAndRewardItems(Player player, int itemType, int rewardType, int npcId) {
        // Retrieve array with items to check.
        final int[] itemsToCheck = SCROLLS[itemType];

        // Check set of items.
        for (int item = itemsToCheck[0]; item <= itemsToCheck[1]; item++) {
            if (!player.getInventory().hasItems(item)) {
                return npcId + ((npcId == WALDERAL) ? "-07a.htm" : "-01.htm");
            }
        }

        // Remove set of items.
        for (int item = itemsToCheck[0]; item <= itemsToCheck[1]; item++) {
            takeItems(player, item, 1);
        }

        final int chance = Rnd.get(100);

        // Retrieve array with rewards.
        for (int[] reward : REWARDS_MATRICE[rewardType]) {
            if (chance < reward[0]) {
                rewardItems(player, reward[1], 1);
                return npcId + "-02.htm";
            }
        }

        return npcId + ((npcId == WALDERAL) ? "-07a.htm" : "-01.htm");
    }
}