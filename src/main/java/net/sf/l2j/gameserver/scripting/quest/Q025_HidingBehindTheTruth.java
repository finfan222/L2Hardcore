package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Q025_HidingBehindTheTruth extends Quest {
    private static final String QUEST_NAME = "Q025_HidingBehindTheTruth";

    // Items
    private static final int FOREST_OF_DEADMAN_MAP = 7063;
    private static final int CONTRACT = 7066;
    private static final int LIDIA_DRESS = 7155;
    private static final int SUSPICIOUS_TOTEM_DOLL_2 = 7156;
    private static final int GEMSTONE_KEY = 7157;
    private static final int SUSPICIOUS_TOTEM_DOLL_3 = 7158;

    // Rewards
    private static final int EARRING_OF_BLESSING = 874;
    private static final int RING_OF_BLESSING = 905;
    private static final int NECKLACE_OF_BLESSING = 936;

    // NPCs
    private static final int AGRIPEL = 31348;
    private static final int BENEDICT = 31349;
    private static final int MYSTERIOUS_WIZARD = 31522;
    private static final int TOMBSTONE = 31531;
    private static final int MAID_OF_LIDIA = 31532;
    private static final int BROKEN_BOOKSHELF_1 = 31533;
    private static final int BROKEN_BOOKSHELF_2 = 31534;
    private static final int BROKEN_BOOKSHELF_3 = 31535;
    private static final int COFFIN = 31536;

    // Monsters
    private static final int TRIOL_PAWN = 27218;

    // Spawns
    private static final Map<Integer, SpawnLocation> TRIOL_SPAWNS = new HashMap<>(3);

    {
        TRIOL_SPAWNS.put(BROKEN_BOOKSHELF_1, new SpawnLocation(47142, -35941, -1623, 0));
        TRIOL_SPAWNS.put(BROKEN_BOOKSHELF_2, new SpawnLocation(50055, -47020, -3396, 0));
        TRIOL_SPAWNS.put(BROKEN_BOOKSHELF_3, new SpawnLocation(59712, -47568, -2720, 0));
    }

    // Sound
    private static final String SOUND_HORROR_1 = "SkillSound5.horror_01";
    private static final String SOUND_HORROR_2 = "AmdSound.dd_horror_02";
    private static final String SOUND_CRY = "ChrSound.FDElf_Cry";

    private final Map<Npc, Attackable> _triolPawns = new ConcurrentHashMap<>(3);
    private Npc _coffin;

    public Q025_HidingBehindTheTruth() {
        super(25, "Hiding Behind the Truth");

        // Note: FOREST_OF_DEADMAN_MAP and SUSPICIOUS_TOTEM_DOLL_2 are items from previous quests, should not be added.
        setItemsIds(CONTRACT, LIDIA_DRESS, GEMSTONE_KEY, SUSPICIOUS_TOTEM_DOLL_3);

        addStartNpc(BENEDICT);
        addTalkId(AGRIPEL, BENEDICT, MYSTERIOUS_WIZARD, TOMBSTONE, MAID_OF_LIDIA, BROKEN_BOOKSHELF_1, BROKEN_BOOKSHELF_2, BROKEN_BOOKSHELF_3, COFFIN);
        addFirstTalkId(MAID_OF_LIDIA);

        addKillId(TRIOL_PAWN);
        addDecayId(TRIOL_PAWN, COFFIN);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 66;
        condition.quests = new QuestDetail[]{QuestDetail.builder().id(24).build()};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        // Benedict
        if (event.equalsIgnoreCase("31349-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.set("state", 1);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("31349-04.htm")) {
            // Suspicious Totem is lost, redirect to Mysterious Wizard to obtain it again
            if (!player.getInventory().hasItems(SUSPICIOUS_TOTEM_DOLL_2)) {
                htmltext = "31349-05.htm";
                if (st.getCond() == 1) {
                    st.setCond(2);
                    playSound(player, SOUND_MIDDLE);
                }
            }
        } else if (event.equalsIgnoreCase("31349-10.htm")) {
            st.set("state", 2);
            st.setCond(4);
            playSound(player, SOUND_MIDDLE);
        }
        // Agripel
        else if (event.equalsIgnoreCase("31348-02.htm")) {
            st.set("state", 3);
            takeItems(player, SUSPICIOUS_TOTEM_DOLL_2, -1);
        } else if (event.equalsIgnoreCase("31348-08.htm")) {
            st.set("state", 6);
            st.setCond(5);
            playSound(player, SOUND_MIDDLE);
            giveItems(player, GEMSTONE_KEY, 1);
        } else if (event.equalsIgnoreCase("31348-10.htm")) {
            st.set("state", 21);
            takeItems(player, SUSPICIOUS_TOTEM_DOLL_3, -1);
        } else if (event.equalsIgnoreCase("31348-13.htm")) {
            st.set("state", 22);
        } else if (event.equalsIgnoreCase("31348-16.htm")) {
            st.set("state", 23);
            st.setCond(17);
            playSound(player, SOUND_MIDDLE);
        } else if (event.equalsIgnoreCase("31348-17.htm")) {
            st.set("state", 24);
            st.setCond(18);
            playSound(player, SOUND_MIDDLE);
        }
        // Mysterious Wizard
        else if (event.equalsIgnoreCase("31522-04.htm")) {
            st.set("state", 7);
            st.setCond(6);
            playSound(player, SOUND_MIDDLE);
        } else if (event.equalsIgnoreCase("31522-10.htm")) {
            st.set("state", 19);
        } else if (event.equalsIgnoreCase("31522-13.htm")) {
            st.set("state", 20);
            st.setCond(16);
            playSound(player, SOUND_MIDDLE);
        } else if (event.equalsIgnoreCase("31522-16.htm")) {
            takeItems(player, FOREST_OF_DEADMAN_MAP, -1);
            giveItems(player, EARRING_OF_BLESSING, 1);
            giveItems(player, NECKLACE_OF_BLESSING, 1);
            rewardExpAndSp(player, 1607062, 0);
            playSound(player, SOUND_FINISH);
            st.exitQuest(false);
        }
        // Broken Bookshelf 1, 2 and 3
        else if (event.equalsIgnoreCase("3153x-05.htm")) {
            // Get bookshelf flags.
            final String npcId = String.valueOf(npc.getNpcId());
            st.set(npcId, 1);

            // Check bookshelves.
            if (st.getInteger("31533") + st.getInteger("31534") + st.getInteger("31535") == 3) {
                // All are open, clear bookshelf identifier, mark gem box location.
                st.unset("31533");
                st.unset("31534");
                st.unset("31535");
                st.set("bookshelf", npcId);
                st.set("state", 8);
                playSound(player, SOUND_HORROR_2);
            } else
            // Not all bookshelves are opened yet.
            {
                htmltext = "3153x-03.htm";
            }
        } else if (event.equalsIgnoreCase("3153x-07.htm")) {
            if (!player.getInventory().hasItems(SUSPICIOUS_TOTEM_DOLL_3)) {
                Attackable triolPawn = _triolPawns.get(npc);
                if (triolPawn == null) {
                    triolPawn = (Attackable) addSpawn(TRIOL_PAWN, TRIOL_SPAWNS.get(npc.getNpcId()), false, 120000, true);
                    triolPawn.forceAttack(player, 20000);
                    triolPawn.setScriptValue(player.getObjectId());
                    triolPawn.broadcastNpcSay(NpcStringId.ID_2550, player.getName());

                    _triolPawns.put(npc, triolPawn);

                    st.setCond(7);
                    playSound(player, SOUND_MIDDLE);
                } else if (triolPawn.getScriptValue() == player.getObjectId()) {
                    htmltext = "3153x-08.htm";
                } else {
                    htmltext = "3153x-09.htm";
                }
            } else {
                htmltext = "3153x-10.htm";
            }
        } else if (event.equalsIgnoreCase("3153x-11.htm")) {
            st.unset("bookshelf");
            st.set("state", 9);
            st.setCond(9);
            playSound(player, SOUND_MIDDLE);
            takeItems(player, GEMSTONE_KEY, -1);
            giveItems(player, CONTRACT, 1);
        }
        // Maid of Lidia
        else if (event.equalsIgnoreCase("31532-02.htm")) {
            st.set("state", 10);
            takeItems(player, CONTRACT, -1);
        } else if (event.equalsIgnoreCase("31532-07.htm")) {
            st.set("state", 11);
            st.setCond(11);
            playSound(player, SOUND_HORROR_1);
        } else if (event.equalsIgnoreCase("31532-12.htm")) {
            final int sorrow = st.getInteger("sorrow");
            if (sorrow > 0) {
                htmltext = "31532-11.htm";
                st.set("sorrow", sorrow - 1);
                playSound(player, SOUND_CRY);
            } else {
                st.unset("sorrow");
                st.set("state", 14);
            }
        } else if (event.equalsIgnoreCase("31532-17.htm")) {
            st.set("state", 15);
        } else if (event.equalsIgnoreCase("31532-21.htm")) {
            st.set("state", 16);
            st.setCond(15);
            playSound(player, SOUND_MIDDLE);
        } else if (event.equalsIgnoreCase("31532-25.htm")) {
            takeItems(player, FOREST_OF_DEADMAN_MAP, -1);
            giveItems(player, EARRING_OF_BLESSING, 1);
            giveItems(player, RING_OF_BLESSING, 2);
            rewardExpAndSp(player, 1607062, 0);
            playSound(player, SOUND_FINISH);
            st.exitQuest(false);
        }
        // Tombstone
        else if (event.equalsIgnoreCase("31531-02.htm")) {
            st.setCond(12);
            playSound(player, SOUND_MIDDLE);
            if (_coffin == null) {
                _coffin = addSpawn(COFFIN, 60104, -35820, -681, 0, false, 20000, true);
            }
        }

        return htmltext;
    }

    @Override
    public String onDecay(Npc npc) {
        if (_coffin == npc) {
            _coffin = null;
        } else {
            _triolPawns.values().remove(npc);
        }

        return null;
    }

    @Override
    public String onFirstTalk(Npc npc, Player player) {
        final QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st != null && st.getInteger("state") == 11) {
            playSound(player, SOUND_HORROR_1);
            return "31532-08.htm";
        }

        npc.showChatWindow(player);
        return null;
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
                htmltext = condition.validateQuests(player) && condition.validateLevel(player) ? "31349-01.htm" : "31349-02.htm";
                break;

            case STARTED:
                final int state = st.getInteger("state");
                switch (npc.getNpcId()) {
                    case BENEDICT:
                        if (state == 1) {
                            htmltext = "31349-03a.htm";
                        } else if (state > 1) {
                            htmltext = "31349-10.htm";
                        }
                        break;

                    case AGRIPEL:
                        if (state == 2) {
                            htmltext = "31348-01.htm";
                        } else if (state > 2 && state < 6) {
                            htmltext = "31348-02.htm";
                        } else if (state > 5 && state < 20) {
                            htmltext = "31348-08a.htm";
                        } else if (state == 20) {
                            htmltext = "31348-09.htm";
                        } else if (state == 21) {
                            htmltext = "31348-10a.htm";
                        } else if (state == 22) {
                            htmltext = "31348-15.htm";
                        } else if (state == 23) {
                            htmltext = "31348-18.htm";
                        } else if (state == 24) {
                            htmltext = "31348-19.htm";
                        }
                        break;

                    case MYSTERIOUS_WIZARD:
                        if (state == 1) {
                            if (!player.getInventory().hasItems(SUSPICIOUS_TOTEM_DOLL_2)) {
                                htmltext = "31522-01.htm";
                                st.setCond(3);
                                playSound(player, SOUND_MIDDLE);
                                giveItems(player, SUSPICIOUS_TOTEM_DOLL_2, 1);
                            } else {
                                htmltext = "31522-02.htm";
                            }
                        } else if (state > 1 && state < 6) {
                            htmltext = "31522-02.htm";
                        } else if (state == 6) {
                            htmltext = "31522-03.htm";
                        } else if (state > 6 && state < 9) {
                            htmltext = "31522-04.htm";
                        } else if (state == 9) {
                            htmltext = "31522-06.htm";

                            if (st.getCond() != 10 && player.getInventory().hasItems(CONTRACT)) {
                                st.setCond(10);
                                playSound(player, SOUND_MIDDLE);
                            }
                        } else if (state > 9 && state < 16) {
                            htmltext = "31522-06.htm";
                        } else if (state == 16) {
                            htmltext = "31522-06a.htm";
                        } else if (state == 19) {
                            htmltext = "31522-11.htm";
                        } else if (state > 19 && state < 23) {
                            htmltext = "31522-14.htm";
                        } else if (state == 23) {
                            htmltext = "31522-15a.htm";
                        } else if (state == 24) {
                            htmltext = "31522-15.htm";
                        }
                        break;

                    case BROKEN_BOOKSHELF_1:
                    case BROKEN_BOOKSHELF_2:
                    case BROKEN_BOOKSHELF_3:
                        if (state == 7) {
                            // Investigating bookshelves, check if current one has been opened.
                            if (st.getInteger(String.valueOf(npc.getNpcId())) == 0) {
                                htmltext = "3153x-01.htm";
                            } else {
                                htmltext = "3153x-03.htm";
                            }
                        } else if (state == 8) {
                            // Gem box has been found. Check if in this bookshelf.
                            if (st.getInteger("bookshelf") == npc.getNpcId()) {
                                htmltext = "3153x-05.htm";
                            } else {
                                htmltext = "3153x-03.htm";
                            }
                        } else if (state > 8) {
                            htmltext = "3153x-02.htm";
                        }
                        break;

                    case MAID_OF_LIDIA:
                        if (state == 9) {
                            htmltext = "31532-01.htm";
                        } else if (state > 9 && state < 12) {
                            htmltext = "31532-03.htm";
                        } else if (state == 12) {
                            htmltext = "31532-09.htm";
                            st.set("sorrow", 4);
                            st.set("state", 13);
                            st.setCond(14);
                            playSound(player, SOUND_MIDDLE);
                            takeItems(player, LIDIA_DRESS, -1);
                        } else if (state == 13) {
                            htmltext = "31532-10.htm";
                            playSound(player, SOUND_CRY);
                        } else if (state == 14) {
                            htmltext = "31532-12.htm";
                        } else if (state == 15) {
                            htmltext = "31532-17.htm";
                        } else if (state > 15 && state < 23) {
                            htmltext = "31532-21.htm";
                        } else if (state == 23) {
                            htmltext = "31532-23.htm";
                        } else if (state == 24) {
                            htmltext = "31532-24.htm";
                        }
                        break;

                    case TOMBSTONE:
                        if (state == 11) {
                            htmltext = (_coffin == null) ? "31531-01.htm" : "31531-02.htm";
                        } else if (state > 11) {
                            htmltext = "31531-03.htm";
                        }
                        break;

                    case COFFIN:
                        if (state == 11) {
                            htmltext = "31536-01.htm";
                            st.set("state", 12);
                            st.setCond(13);
                            playSound(player, SOUND_MIDDLE);
                            giveItems(player, LIDIA_DRESS, 1);

                            _coffin.deleteMe();
                        }
                        break;
                }
                break;

            case COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = checkPlayerCondition(player, npc, 7);
        if (st == null) {
            return null;
        }

        if (player.getObjectId() != npc.getScriptValue()) {
            return null;
        }

        if (dropItemsAlways(player, SUSPICIOUS_TOTEM_DOLL_3, 1, 1)) {
            st.setCond(8);
            npc.broadcastNpcSay(NpcStringId.ID_2551);
        }

        return null;
    }
}