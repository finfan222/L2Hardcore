package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q294_CovertBusiness extends Quest {
    private static final String QUEST_NAME = "Q294_CovertBusiness";

    // Item
    private static final int BAT_FANG = 1491;

    // Reward
    private static final int RING_OF_RACCOON = 1508;

    public Q294_CovertBusiness() {
        super(294, "Covert Business");

        setItemsIds(BAT_FANG);

        addStartNpc(30534); // Keef
        addTalkId(30534);

        addKillId(20370, 20480); // Barded Bat, Blade Bat
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 10;
        condition.races = new ClassRace[]{ClassRace.DWARF};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30534-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
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
                if (!condition.validateRace(player)) {
                    htmltext = "30534-00.htm";
                } else if (!condition.validateLevel(player)) {
                    htmltext = "30534-01.htm";
                } else {
                    htmltext = "30534-02.htm";
                }
                break;

            case STARTED:
                if (st.getCond() == 1) {
                    htmltext = "30534-04.htm";
                } else {
                    if (!player.getInventory().hasItems(RING_OF_RACCOON)) {
                        htmltext = "30534-05.htm";
                        takeItems(player, BAT_FANG, -1);
                        giveItems(player, RING_OF_RACCOON, 1);
                    } else {
                        htmltext = "30534-06.htm";
                        takeItems(player, BAT_FANG, -1);
                        rewardItems(player, 57, 2400);
                    }
                    rewardExpAndSp(player, 0, 600);
                    playSound(player, SOUND_FINISH);
                    st.exitQuest(true);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();

        final QuestState st = checkPlayerCondition(player, npc, 1);
        if (st == null) {
            return null;
        }

        int count = 1;
        final int chance = Rnd.get(10);
        final boolean isBarded = (npc.getNpcId() == 20370);

        if (chance < 3) {
            count++;
        } else if (chance < ((isBarded) ? 5 : 6)) {
            count += 2;
        } else if (isBarded && chance < 7) {
            count += 3;
        }

        if (dropItemsAlways(player, BAT_FANG, count, 100)) {
            st.setCond(2);
        }

        return null;
    }
}