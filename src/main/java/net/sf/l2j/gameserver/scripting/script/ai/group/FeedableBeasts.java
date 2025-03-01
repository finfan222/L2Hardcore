package net.sf.l2j.gameserver.scripting.script.ai.group;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.TamedBeast;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.quest.Q020_BringUpWithLove;
import net.sf.l2j.gameserver.scripting.quest.Q655_AGrandPlanForTamingWildBeasts;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeedableBeasts extends AttackableAIScript {
    private static final int GOLDEN_SPICE = 6643;
    private static final int CRYSTAL_SPICE = 6644;
    private static final int SKILL_GOLDEN_SPICE = 2188;
    private static final int SKILL_CRYSTAL_SPICE = 2189;

    private static final int CRYSTAL_OF_PURITY = 8084;
    private static final int REQUIRED_CRYSTAL_COUNT = 10;

    private static final int[] TAMED_BEASTS =
        {
            16013,
            16014,
            16015,
            16016,
            16017,
            16018
        };

    private static final int[] FEEDABLE_BEASTS =
        {
            21451,
            21452,
            21453,
            21454,
            21455,
            21456,
            21457,
            21458,
            21459,
            21460,
            21461,
            21462,
            21463,
            21464,
            21465,
            21466,
            21467,
            21468,
            21469, // Alpen Kookaburra
            21470,
            21471,
            21472,
            21473,
            21474,
            21475,
            21476,
            21477,
            21478,
            21479,
            21480,
            21481,
            21482,
            21483,
            21484,
            21485,
            21486,
            21487,
            21488, // Alpen Buffalo
            21489,
            21490,
            21491,
            21492,
            21493,
            21494,
            21495,
            21496,
            21497,
            21498,
            21499,
            21500,
            21501,
            21502,
            21503,
            21504,
            21505,
            21506,
            21507, // Alpen Cougar
            21824,
            21825,
            21826,
            21827,
            21828,
            21829
            // Alpen Kookaburra, Buffalo, Cougar
        };

    private static final Map<Integer, Integer> MAD_COW_POLYMORPH = new HashMap<>(6);

    static {
        MAD_COW_POLYMORPH.put(21824, 21468);
        MAD_COW_POLYMORPH.put(21825, 21469);
        MAD_COW_POLYMORPH.put(21826, 21487);
        MAD_COW_POLYMORPH.put(21827, 21488);
        MAD_COW_POLYMORPH.put(21828, 21506);
        MAD_COW_POLYMORPH.put(21829, 21507);
    }

    private static final String[][] TEXT =
        {
            {
                "What did you just do to me?",
                "You want to tame me, huh?",
                "Do not give me this. Perhaps you will be in danger.",
                "Bah bah. What is this unpalatable thing?",
                "My belly has been complaining. This hit the spot.",
                "What is this? Can I eat it?",
                "You don't need to worry about me.",
                "Delicious food, thanks.",
                "I am starting to like you!",
                "Gulp!"
            },
            {
                "I do not think you have given up on the idea of taming me.",
                "That is just food to me. Perhaps I can eat your hand too.",
                "Will eating this make me fat? Ha ha.",
                "Why do you always feed me?",
                "Do not trust me. I may betray you."
            },
            {
                "Destroy!",
                "Look what you have done!",
                "Strange feeling...! Evil intentions grow in my heart...!",
                "It is happening!",
                "This is sad...Good is sad...!"
            }
        };

    private static final String[] SPAWN_CHATS =
        {
            "$s1, will you show me your hideaway?",
            "$s1, whenever I look at spice, I think about you.",
            "$s1, you do not need to return to the village. I will give you strength.",
            "Thanks, $s1. I hope I can help you.",
            "$s1, what can I do to help you?",
        };

    private static final Map<Integer, Integer> FEED_INFO = new ConcurrentHashMap<>();
    private static final Map<Integer, GrowthCapableMob> GROWTH_CAPABLE_MOBS = new HashMap<>();

    private static class GrowthCapableMob {
        private final int _growthLevel;
        private final int _chance;

        private final Map<Integer, int[][]> _spiceToMob = new HashMap<>();

        public GrowthCapableMob(int growthLevel, int chance) {
            _growthLevel = growthLevel;
            _chance = chance;
        }

        public void addMobs(int spice, int[][] Mobs) {
            _spiceToMob.put(spice, Mobs);
        }

        public Integer getMob(int spice, int mobType, int classType) {
            if (_spiceToMob.containsKey(spice)) {
                return _spiceToMob.get(spice)[mobType][classType];
            }

            return null;
        }

        public Integer getRandomMob(int spice) {
            int[][] temp;
            temp = _spiceToMob.get(spice);
            int rand = Rnd.get(temp[0].length);
            return temp[0][rand];
        }

        public Integer getChance() {
            return _chance;
        }

        public Integer getGrowthLevel() {
            return _growthLevel;
        }
    }

    public FeedableBeasts() {
        super("ai/group");

        GrowthCapableMob temp;

        final int[][] Kookabura_0_Gold =
            {
                {
                    21452,
                    21453,
                    21454,
                    21455
                }
            };
        final int[][] Kookabura_0_Crystal =
            {
                {
                    21456,
                    21457,
                    21458,
                    21459
                }
            };
        final int[][] Kookabura_1_Gold_1 =
            {
                {
                    21460,
                    21462
                }
            };
        final int[][] Kookabura_1_Gold_2 =
            {
                {
                    21461,
                    21463
                }
            };
        final int[][] Kookabura_1_Crystal_1 =
            {
                {
                    21464,
                    21466
                }
            };
        final int[][] Kookabura_1_Crystal_2 =
            {
                {
                    21465,
                    21467
                }
            };
        final int[][] Kookabura_2_1 =
            {
                {
                    21468,
                    21824
                },
                {
                    16017,
                    16018
                }
            };
        final int[][] Kookabura_2_2 =
            {
                {
                    21469,
                    21825
                },
                {
                    16017,
                    16018
                }
            };

        final int[][] Buffalo_0_Gold =
            {
                {
                    21471,
                    21472,
                    21473,
                    21474
                }
            };
        final int[][] Buffalo_0_Crystal =
            {
                {
                    21475,
                    21476,
                    21477,
                    21478
                }
            };
        final int[][] Buffalo_1_Gold_1 =
            {
                {
                    21479,
                    21481
                }
            };
        final int[][] Buffalo_1_Gold_2 =
            {
                {
                    21481,
                    21482
                }
            };
        final int[][] Buffalo_1_Crystal_1 =
            {
                {
                    21483,
                    21485
                }
            };
        final int[][] Buffalo_1_Crystal_2 =
            {
                {
                    21484,
                    21486
                }
            };
        final int[][] Buffalo_2_1 =
            {
                {
                    21487,
                    21826
                },
                {
                    16013,
                    16014
                }
            };
        final int[][] Buffalo_2_2 =
            {
                {
                    21488,
                    21827
                },
                {
                    16013,
                    16014
                }
            };

        final int[][] Cougar_0_Gold =
            {
                {
                    21490,
                    21491,
                    21492,
                    21493
                }
            };
        final int[][] Cougar_0_Crystal =
            {
                {
                    21494,
                    21495,
                    21496,
                    21497
                }
            };
        final int[][] Cougar_1_Gold_1 =
            {
                {
                    21498,
                    21500
                }
            };
        final int[][] Cougar_1_Gold_2 =
            {
                {
                    21499,
                    21501
                }
            };
        final int[][] Cougar_1_Crystal_1 =
            {
                {
                    21502,
                    21504
                }
            };
        final int[][] Cougar_1_Crystal_2 =
            {
                {
                    21503,
                    21505
                }
            };
        final int[][] Cougar_2_1 =
            {
                {
                    21506,
                    21828
                },
                {
                    16015,
                    16016
                }
            };
        final int[][] Cougar_2_2 =
            {
                {
                    21507,
                    21829
                },
                {
                    16015,
                    16016
                }
            };

        // Alpen Kookabura
        temp = new GrowthCapableMob(0, 100);
        temp.addMobs(GOLDEN_SPICE, Kookabura_0_Gold);
        temp.addMobs(CRYSTAL_SPICE, Kookabura_0_Crystal);
        GROWTH_CAPABLE_MOBS.put(21451, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Kookabura_1_Gold_1);
        GROWTH_CAPABLE_MOBS.put(21452, temp);
        GROWTH_CAPABLE_MOBS.put(21454, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Kookabura_1_Gold_2);
        GROWTH_CAPABLE_MOBS.put(21453, temp);
        GROWTH_CAPABLE_MOBS.put(21455, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Kookabura_1_Crystal_1);
        GROWTH_CAPABLE_MOBS.put(21456, temp);
        GROWTH_CAPABLE_MOBS.put(21458, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Kookabura_1_Crystal_2);
        GROWTH_CAPABLE_MOBS.put(21457, temp);
        GROWTH_CAPABLE_MOBS.put(21459, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Kookabura_2_1);
        GROWTH_CAPABLE_MOBS.put(21460, temp);
        GROWTH_CAPABLE_MOBS.put(21462, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Kookabura_2_2);
        GROWTH_CAPABLE_MOBS.put(21461, temp);
        GROWTH_CAPABLE_MOBS.put(21463, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Kookabura_2_1);
        GROWTH_CAPABLE_MOBS.put(21464, temp);
        GROWTH_CAPABLE_MOBS.put(21466, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Kookabura_2_2);
        GROWTH_CAPABLE_MOBS.put(21465, temp);
        GROWTH_CAPABLE_MOBS.put(21467, temp);

        // Alpen Buffalo
        temp = new GrowthCapableMob(0, 100);
        temp.addMobs(GOLDEN_SPICE, Buffalo_0_Gold);
        temp.addMobs(CRYSTAL_SPICE, Buffalo_0_Crystal);
        GROWTH_CAPABLE_MOBS.put(21470, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Buffalo_1_Gold_1);
        GROWTH_CAPABLE_MOBS.put(21471, temp);
        GROWTH_CAPABLE_MOBS.put(21473, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Buffalo_1_Gold_2);
        GROWTH_CAPABLE_MOBS.put(21472, temp);
        GROWTH_CAPABLE_MOBS.put(21474, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Buffalo_1_Crystal_1);
        GROWTH_CAPABLE_MOBS.put(21475, temp);
        GROWTH_CAPABLE_MOBS.put(21477, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Buffalo_1_Crystal_2);
        GROWTH_CAPABLE_MOBS.put(21476, temp);
        GROWTH_CAPABLE_MOBS.put(21478, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Buffalo_2_1);
        GROWTH_CAPABLE_MOBS.put(21479, temp);
        GROWTH_CAPABLE_MOBS.put(21481, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Buffalo_2_2);
        GROWTH_CAPABLE_MOBS.put(21480, temp);
        GROWTH_CAPABLE_MOBS.put(21482, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Buffalo_2_1);
        GROWTH_CAPABLE_MOBS.put(21483, temp);
        GROWTH_CAPABLE_MOBS.put(21485, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Buffalo_2_2);
        GROWTH_CAPABLE_MOBS.put(21484, temp);
        GROWTH_CAPABLE_MOBS.put(21486, temp);

        // Alpen Cougar
        temp = new GrowthCapableMob(0, 100);
        temp.addMobs(GOLDEN_SPICE, Cougar_0_Gold);
        temp.addMobs(CRYSTAL_SPICE, Cougar_0_Crystal);
        GROWTH_CAPABLE_MOBS.put(21489, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Cougar_1_Gold_1);
        GROWTH_CAPABLE_MOBS.put(21490, temp);
        GROWTH_CAPABLE_MOBS.put(21492, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(GOLDEN_SPICE, Cougar_1_Gold_2);
        GROWTH_CAPABLE_MOBS.put(21491, temp);
        GROWTH_CAPABLE_MOBS.put(21493, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Cougar_1_Crystal_1);
        GROWTH_CAPABLE_MOBS.put(21494, temp);
        GROWTH_CAPABLE_MOBS.put(21496, temp);

        temp = new GrowthCapableMob(1, 40);
        temp.addMobs(CRYSTAL_SPICE, Cougar_1_Crystal_2);
        GROWTH_CAPABLE_MOBS.put(21495, temp);
        GROWTH_CAPABLE_MOBS.put(21497, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Cougar_2_1);
        GROWTH_CAPABLE_MOBS.put(21498, temp);
        GROWTH_CAPABLE_MOBS.put(21500, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(GOLDEN_SPICE, Cougar_2_2);
        GROWTH_CAPABLE_MOBS.put(21499, temp);
        GROWTH_CAPABLE_MOBS.put(21501, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Cougar_2_1);
        GROWTH_CAPABLE_MOBS.put(21502, temp);
        GROWTH_CAPABLE_MOBS.put(21504, temp);

        temp = new GrowthCapableMob(2, 25);
        temp.addMobs(CRYSTAL_SPICE, Cougar_2_2);
        GROWTH_CAPABLE_MOBS.put(21503, temp);
        GROWTH_CAPABLE_MOBS.put(21505, temp);
    }

    @Override
    protected void registerNpcs() {
        addEventIds(FEEDABLE_BEASTS, ScriptEventType.ON_KILL, ScriptEventType.ON_SKILL_SEE);
    }

    public void spawnNext(Npc npc, int growthLevel, Player player, int food) {
        int npcId = npc.getNpcId();
        int nextNpcId = 0;

        // Find the next mob to spawn, based on the current npcId, growthlevel, and food.
        if (growthLevel == 2) {
            // If tamed, the mob that will spawn depends on the class type (fighter/mage) of the player!
            if (Rnd.get(2) == 0) {
                if (player.isMageClass()) {
                    nextNpcId = GROWTH_CAPABLE_MOBS.get(npcId).getMob(food, 1, 1);
                } else {
                    nextNpcId = GROWTH_CAPABLE_MOBS.get(npcId).getMob(food, 1, 0);
                }
            } else {
                /*
                 * If not tamed, there is a small chance that have "mad cow" disease. that is a stronger-than-normal animal that attacks its feeder
                 */
                if (Rnd.get(5) == 0) {
                    nextNpcId = GROWTH_CAPABLE_MOBS.get(npcId).getMob(food, 0, 1);
                } else {
                    nextNpcId = GROWTH_CAPABLE_MOBS.get(npcId).getMob(food, 0, 0);
                }
            }
        }
        // All other levels of growth are straight-forward
        else {
            nextNpcId = GROWTH_CAPABLE_MOBS.get(npcId).getRandomMob(food);
        }

        // Remove the feedinfo of the mob that got despawned, if any
        if (FEED_INFO.getOrDefault(npc.getObjectId(), 0) == player.getObjectId()) {
            FEED_INFO.remove(npc.getObjectId());
        }

        // Despawn the old mob
        npc.deleteMe();

        // if this is finally a trained mob, then despawn any other trained mobs that the player might have and initialize the Tamed Beast.
        if (ArraysUtil.contains(TAMED_BEASTS, nextNpcId)) {
            if (player.getTamedBeast() != null) {
                player.getTamedBeast().deleteMe();
            }

            NpcTemplate template = NpcData.getInstance().getTemplate(nextNpcId);
            TamedBeast nextNpc = new TamedBeast(IdFactory.getInstance().getNextId(), template, player, food, npc.getPosition());
            nextNpc.forceRunStance();

            // Support for "Bring Up With Love" (020) quest.
            QuestState st = player.getQuestList().getQuestState(Q020_BringUpWithLove.QUEST_NAME);
            if (st != null && Rnd.get(100) < 5 && !player.getInventory().hasItems(7185)) {
                giveItems(player, 7185, 1);
                st.setCond(2);
            }

            // Support for "A Grand Plan for Taming Wild Beasts" (655) quest.
            testCrystalOfPurity(player, npc);

            // Also, perform a rare random chat
            int rand = Rnd.get(20);
            if (rand < 5) {
                npc.broadcastNpcSay(SPAWN_CHATS[rand].replace("$s1", player.getName()));
            }
        } else {
            // If not trained, the newly spawned mob will automatically be aggro against its feeder
            final Npc newNpc = addSpawn(nextNpcId, npc, false, 0, false);

            if (MAD_COW_POLYMORPH.containsKey(nextNpcId)) {
                startQuestTimer("polymorph Mad Cow", newNpc, player, 10000);
            }

            // Register the player in the feedinfo for the mob that just spawned
            FEED_INFO.put(newNpc.getObjectId(), player.getObjectId());

            newNpc.forceAttack(player, 200);
        }
    }

    @Override
    public String onTimer(String name, Npc npc, Player player) {
        if (name.equalsIgnoreCase("polymorph Mad Cow")) {
            if (MAD_COW_POLYMORPH.containsKey(npc.getNpcId())) {
                // remove the feed info from the previous mob
                if (FEED_INFO.getOrDefault(npc.getObjectId(), 0) == player.getObjectId()) {
                    FEED_INFO.remove(npc.getObjectId());
                }

                // spawn the new mob
                final Npc newNpc = addSpawn(MAD_COW_POLYMORPH.get(npc.getNpcId()), npc, false, 0, false);
                newNpc.forceAttack(player, 200);

                // register the player in the feedinfo for the mob that just spawned
                FEED_INFO.put(newNpc.getObjectId(), player.getObjectId());

                // despawn the mad cow
                npc.deleteMe();
            }
        }

        return super.onTimer(name, npc, player);
    }

    @Override
    public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet) {
        if (!ArraysUtil.contains(targets, npc)) {
            return super.onSkillSee(npc, caster, skill, targets, isPet);
        }

        // Gather some values on local variables
        int npcId = npc.getNpcId();
        int skillId = skill.getId();

        // Check if the npc and skills used are valid for this script. Exit if invalid.
        if (!ArraysUtil.contains(FEEDABLE_BEASTS, npcId) || (skillId != SKILL_GOLDEN_SPICE && skillId != SKILL_CRYSTAL_SPICE)) {
            return super.onSkillSee(npc, caster, skill, targets, isPet);
        }

        // First gather some values on local variables
        int objectId = npc.getObjectId();
        int growthLevel = 3; // if a mob is in FEEDABLE_BEASTS but not in GROWTH_CAPABLE_MOBS, then it's at max growth (3)

        if (GROWTH_CAPABLE_MOBS.containsKey(npcId)) {
            growthLevel = GROWTH_CAPABLE_MOBS.get(npcId).getGrowthLevel();
        }

        // Prevent exploit which allows 2 players to simultaneously raise the same 0-growth beast
        // If the mob is at 0th level (when it still listens to all feeders) lock it to the first feeder!
        if (growthLevel == 0 && FEED_INFO.containsKey(objectId)) {
            return super.onSkillSee(npc, caster, skill, targets, isPet);
        }

        FEED_INFO.put(objectId, caster.getObjectId());

        int food = 0;
        if (skillId == SKILL_GOLDEN_SPICE) {
            food = GOLDEN_SPICE;
        } else if (skillId == SKILL_CRYSTAL_SPICE) {
            food = CRYSTAL_SPICE;
        }

        // Display the social action of the beast eating the food.
        npc.broadcastPacket(new SocialAction(npc, 2));

        // If the pet can grow
        if (GROWTH_CAPABLE_MOBS.containsKey(npcId)) {
            // Do nothing if this mob doesn't eat the specified food (food gets consumed but has no effect).
            if (GROWTH_CAPABLE_MOBS.get(npcId).getMob(food, 0, 0) == null) {
                return super.onSkillSee(npc, caster, skill, targets, isPet);
            }

            // Rare random talk...
            if (Rnd.get(20) == 0) {
                npc.broadcastNpcSay(Rnd.get(TEXT[growthLevel]));
            }

            if (growthLevel > 0 && FEED_INFO.getOrDefault(objectId, 0) != caster.getObjectId()) {
                // check if this is the same player as the one who raised it from growth 0.
                // if no, then do not allow a chance to raise the pet (food gets consumed but has no effect).
                return super.onSkillSee(npc, caster, skill, targets, isPet);
            }

            // Polymorph the mob, with a certain chance, given its current growth level
            if (Rnd.get(100) < GROWTH_CAPABLE_MOBS.get(npcId).getChance()) {
                spawnNext(npc, growthLevel, caster, food);
            }
        }

        return super.onSkillSee(npc, caster, skill, targets, isPet);
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        // Remove the feedinfo of the mob that got killed, if any
        FEED_INFO.remove(npc.getObjectId());

        return super.onKill(npc, killer);
    }

    /**
     * A method used by Q655.
     *
     * @param player
     * @param npc
     */
    private static void testCrystalOfPurity(Player player, Npc npc) {
        // No valid player instance is passed, there is nothing to check.
        if (player == null || npc == null) {
            return;
        }

        // Player killer must be in range with npc.
        if (!player.isIn3DRadius(npc, Config.PARTY_RANGE)) {
            return;
        }

        QuestState st = null;

        // If player is the leader, retrieves directly the qS and bypass others checks
        if (player.isClanLeader()) {
            st = player.getQuestList().getQuestState(Q655_AGrandPlanForTamingWildBeasts.QUEST_NAME);
        } else {
            // Verify if the player got a clan
            final Clan clan = player.getClan();
            if (clan == null) {
                return;
            }

            // Verify if the leader is online
            final Player leader = clan.getLeader().getPlayerInstance();
            if (leader == null) {
                return;
            }

            // Verify if the leader is on the radius of the npc. If true, send leader's quest state.
            if (leader.isIn3DRadius(npc, Config.PARTY_RANGE)) {
                st = leader.getQuestList().getQuestState(Q655_AGrandPlanForTamingWildBeasts.QUEST_NAME);
            }
        }

        // We didn't found any valid QuestState ; stop here.
        if (st == null) {
            return;
        }

        // Condition exists? Condition has correct value?
        if (st.getCond() != 1) {
            return;
        }

        // Reward clan leader with a crystal of purity. Once reached 10, trigger cond 2.
        if (dropItemsAlways(player, CRYSTAL_OF_PURITY, 1, REQUIRED_CRYSTAL_COUNT)) {
            st.setCond(2);
        }
    }
}