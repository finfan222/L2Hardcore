package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q168_DeliverSupplies extends Quest {
    private static final String QUEST_NAME = "Q168_DeliverSupplies";

    // Items
    private static final int JENNA_LETTER = 1153;
    private static final int SENTRY_BLADE_1 = 1154;
    private static final int SENTRY_BLADE_2 = 1155;
    private static final int SENTRY_BLADE_3 = 1156;
    private static final int OLD_BRONZE_SWORD = 1157;

    // NPCs
    private static final int JENNA = 30349;
    private static final int ROSELYN = 30355;
    private static final int KRISTIN = 30357;
    private static final int HARANT = 30360;

    public Q168_DeliverSupplies() {
        super(168, "Deliver Supplies");

        setItemsIds(JENNA_LETTER, SENTRY_BLADE_1, SENTRY_BLADE_2, SENTRY_BLADE_3, OLD_BRONZE_SWORD);

        addStartNpc(JENNA);
        addTalkId(JENNA, ROSELYN, KRISTIN, HARANT);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    protected void initializeConditions() {
        condition.level = 3;
        condition.races = new ClassRace[]{ClassRace.DARK_ELF};
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return event;
        }

        if (event.equalsIgnoreCase("30349-03.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, event);
            st.setCond(1);
            playSound(player, SOUND_ACCEPT);
            giveItems(player, JENNA_LETTER, 1);
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
                    htmltext = "30349-00.htm";
                } else if (!condition.validateLevel(player)) {
                    htmltext = "30349-01.htm";
                } else {
                    htmltext = "30349-02.htm";
                }
                break;

            case STARTED:
                int cond = st.getCond();
                switch (npc.getNpcId()) {
                    case JENNA:
                        if (cond == 1) {
                            htmltext = "30349-04.htm";
                        } else if (cond == 2) {
                            htmltext = "30349-05.htm";
                            st.setCond(3);
                            playSound(player, SOUND_MIDDLE);
                            takeItems(player, SENTRY_BLADE_1, 1);
                        } else if (cond == 3) {
                            htmltext = "30349-07.htm";
                        } else if (cond == 4) {
                            htmltext = "30349-06.htm";
                            takeItems(player, OLD_BRONZE_SWORD, 2);
                            rewardItems(player, 57, 820);
                            playSound(player, SOUND_FINISH);
                            st.exitQuest(false);
                        }
                        break;

                    case HARANT:
                        if (cond == 1) {
                            htmltext = "30360-01.htm";
                            st.setCond(2);
                            playSound(player, SOUND_MIDDLE);
                            takeItems(player, JENNA_LETTER, 1);
                            giveItems(player, SENTRY_BLADE_1, 1);
                            giveItems(player, SENTRY_BLADE_2, 1);
                            giveItems(player, SENTRY_BLADE_3, 1);
                        } else if (cond == 2) {
                            htmltext = "30360-02.htm";
                        }
                        break;

                    case ROSELYN:
                        if (cond == 3) {
                            if (player.getInventory().hasItems(SENTRY_BLADE_2)) {
                                htmltext = "30355-01.htm";
                                takeItems(player, SENTRY_BLADE_2, 1);
                                giveItems(player, OLD_BRONZE_SWORD, 1);
                                if (player.getInventory().getItemCount(OLD_BRONZE_SWORD) == 2) {
                                    st.setCond(4);
                                    playSound(player, SOUND_MIDDLE);
                                }
                            } else {
                                htmltext = "30355-02.htm";
                            }
                        } else if (cond == 4) {
                            htmltext = "30355-02.htm";
                        }
                        break;

                    case KRISTIN:
                        if (cond == 3) {
                            if (player.getInventory().hasItems(SENTRY_BLADE_3)) {
                                htmltext = "30357-01.htm";
                                takeItems(player, SENTRY_BLADE_3, 1);
                                giveItems(player, OLD_BRONZE_SWORD, 1);
                                if (player.getInventory().getItemCount(OLD_BRONZE_SWORD) == 2) {
                                    st.setCond(4);
                                    playSound(player, SOUND_MIDDLE);
                                }
                            } else {
                                htmltext = "30357-02.htm";
                            }
                        } else if (cond == 4) {
                            htmltext = "30357-02.htm";
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
}