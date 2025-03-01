package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.manager.RaidBossManager;
import net.sf.l2j.gameserver.enums.BossStatus;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.spawn.BossSpawn;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Q610_MagicalPowerOfWater_Part2 extends Quest {
    private static final String QUEST_NAME = "Q610_MagicalPowerOfWater_Part2";

    // Monster
    private static final int SOUL_OF_WATER_ASHUTAR = 25316;

    // NPCs
    private static final int ASEFA = 31372;
    private static final int VARKAS_HOLY_ALTAR = 31560;

    // Items
    private static final int GREEN_TOTEM = 7238;
    private static final int ICE_HEART_OF_ASHUTAR = 7239;

    // Other
    private static final int CHECK_INTERVAL = 600000; // 10 minutes
    private static final int IDLE_INTERVAL = 2; // (X * CHECK_INTERVAL) = 20 minutes

    private Npc _npc;

    private int _status = -1;

    public Q610_MagicalPowerOfWater_Part2() {
        super(610, "Magical Power of Water - Part 2");

        setItemsIds(ICE_HEART_OF_ASHUTAR);

        addStartNpc(ASEFA);
        addTalkId(ASEFA, VARKAS_HOLY_ALTAR);

        addAttackId(SOUL_OF_WATER_ASHUTAR);
        addKillId(SOUL_OF_WATER_ASHUTAR);

        switch (RaidBossManager.getInstance().getStatus(SOUL_OF_WATER_ASHUTAR)) {
            case ALIVE:
                spawnNpc();
            case DEAD:
                startQuestTimerAtFixedRate("check", null, null, CHECK_INTERVAL);
                break;
        }
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = event;
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return htmltext;
        }

        // Asefa
        if (event.equalsIgnoreCase("31372-04.htm")) {
            if (player.getInventory().hasItems(GREEN_TOTEM)) {
                st.setState(QuestStatus.STARTED);
                st.setCond(1);
                playSound(player, SOUND_ACCEPT);
            } else {
                htmltext = "31372-02.htm";
            }
        } else if (event.equalsIgnoreCase("31372-07.htm")) {
            if (player.getInventory().hasItems(ICE_HEART_OF_ASHUTAR)) {
                takeItems(player, ICE_HEART_OF_ASHUTAR, 1);
                rewardExpAndSp(player, 10000, 0);
                playSound(player, SOUND_FINISH);
                st.exitQuest(true);
            } else {
                htmltext = "31372-08.htm";
            }
        }
        // Varka's Holy Altar
        else if (event.equalsIgnoreCase("31560-02.htm")) {
            if (player.getInventory().hasItems(GREEN_TOTEM)) {
                if (_status < 0) {
                    if (spawnRaid()) {
                        st.setCond(2);
                        playSound(player, SOUND_MIDDLE);
                        takeItems(player, GREEN_TOTEM, 1);
                    }
                } else {
                    htmltext = "31560-04.htm";
                }
            } else {
                htmltext = "31560-03.htm";
            }
        }

        return htmltext;
    }

    @Override
    public String onTimer(String name, Npc npc, Player player) {
        if (name.equals("check")) {
            final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(SOUL_OF_WATER_ASHUTAR);
            if (bs != null && bs.getStatus() == BossStatus.ALIVE) {
                final Npc raid = bs.getBoss();

                if (_status >= 0 && _status-- == 0) {
                    despawnRaid(raid);
                }

                spawnNpc();
            }
        }

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
                if (!player.getInventory().hasItems(GREEN_TOTEM)) {
                    htmltext = "31372-02.htm";
                } else if (player.getStatus().getLevel() < 75 && player.getAllianceWithVarkaKetra() < 2) {
                    htmltext = "31372-03.htm";
                } else {
                    htmltext = "31372-01.htm";
                }
                break;

            case STARTED:
                final int cond = st.getCond();
                switch (npc.getNpcId()) {
                    case ASEFA:
                        htmltext = (cond < 3) ? "31372-05.htm" : "31372-06.htm";
                        break;

                    case VARKAS_HOLY_ALTAR:
                        if (cond == 1) {
                            htmltext = "31560-01.htm";
                        } else if (cond == 2) {
                            htmltext = "31560-05.htm";
                        }
                        break;
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill) {
        final Player player = attacker.getActingPlayer();
        if (player != null) {
            _status = IDLE_INTERVAL;
        }

        return null;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();
        if (player != null) {
            for (QuestState st : getPartyMembers(player, npc, 2)) {
                Player pm = st.getPlayer();
                st.setCond(3);
                playSound(pm, SOUND_MIDDLE);
                giveItems(pm, ICE_HEART_OF_ASHUTAR, 1);
            }
        }

        npc.broadcastNpcSay(NpcStringId.ID_61051);

        // despawn raid (reset info)
        despawnRaid(npc);

        // despawn npc
        if (_npc != null) {
            _npc.deleteMe();
            _npc = null;
        }

        return null;
    }

    private void spawnNpc() {
        // spawn npc, if not spawned
        if (_npc == null) {
            _npc = addSpawn(VARKAS_HOLY_ALTAR, 105452, -36775, -1050, 34000, false, 0, false);
        }
    }

    private boolean spawnRaid() {
        final BossSpawn bs = RaidBossManager.getInstance().getBossSpawn(SOUL_OF_WATER_ASHUTAR);
        if (bs != null && bs.getStatus() == BossStatus.ALIVE) {
            final Npc raid = bs.getBoss();

            // set temporarily spawn location (to provide correct behavior of checkAndReturnToSpawn())
            raid.getSpawn().setLoc(104771, -36993, -1149, Rnd.get(65536));

            // teleport raid from secret place
            raid.teleportTo(104771, -36993, -1149, 100);
            raid.broadcastNpcSay(NpcStringId.ID_61050);

            // set raid status
            _status = IDLE_INTERVAL;

            return true;
        }

        return false;
    }

    private void despawnRaid(Npc raid) {
        // reset spawn location
        raid.getSpawn().setLoc(-105900, -252700, -15542, 0);

        // teleport raid back to secret place
        if (!raid.isDead()) {
            raid.teleportTo(-105900, -252700, -15542, 0);
        }

        // reset raid status
        _status = -1;
    }
}