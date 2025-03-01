package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.Config;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Valakas extends AttackableAIScript {
    private static final BossZone VALAKAS_LAIR = ZoneManager.getInstance().getZoneById(110010, BossZone.class);

    public static final byte DORMANT = 0; // Valakas is spawned and no one has entered yet. Entry is unlocked.
    public static final byte WAITING = 1; // Valakas is spawned and someone has entered, triggering a 30 minute window for additional people to enter. Entry is unlocked.
    public static final byte FIGHTING = 2; // Valakas is engaged in battle, annihilating his foes. Entry is locked.
    public static final byte DEAD = 3; // Valakas has been killed. Entry is locked.

    private static final int[] FRONT_SKILLS =
        {
            4681,
            4682,
            4683,
            4684,
            4689
        };

    private static final int[] BEHIND_SKILLS =
        {
            4685,
            4686,
            4688
        };

    private static final int LAVA_SKIN = 4680;
    private static final int METEOR_SWARM = 4690;

    private static final SpawnLocation[] CUBE_LOC =
        {
            new SpawnLocation(214880, -116144, -1644, 0),
            new SpawnLocation(213696, -116592, -1644, 0),
            new SpawnLocation(212112, -116688, -1644, 0),
            new SpawnLocation(211184, -115472, -1664, 0),
            new SpawnLocation(210336, -114592, -1644, 0),
            new SpawnLocation(211360, -113904, -1644, 0),
            new SpawnLocation(213152, -112352, -1644, 0),
            new SpawnLocation(214032, -113232, -1644, 0),
            new SpawnLocation(214752, -114592, -1644, 0),
            new SpawnLocation(209824, -115568, -1421, 0),
            new SpawnLocation(210528, -112192, -1403, 0),
            new SpawnLocation(213120, -111136, -1408, 0),
            new SpawnLocation(215184, -111504, -1392, 0),
            new SpawnLocation(215456, -117328, -1392, 0),
            new SpawnLocation(213200, -118160, -1424, 0)
        };

    public static final int VALAKAS = 29028;

    private long _timeTracker = 0; // Time tracker for last attack on Valakas.
    private Player _actualVictim; // Actual target of Valakas.

    public Valakas() {
        super("ai/boss");

        final StatSet info = GrandBossManager.getInstance().getStatSet(VALAKAS);

        switch (GrandBossManager.getInstance().getBossStatus(VALAKAS)) {
            case DEAD: // Launch the timer to set DORMANT, or set DORMANT directly if timer expired while offline.
                long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
                if (temp > 0) {
                    startQuestTimer("valakas_unlock", null, null, temp);
                } else {
                    GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);
                }
                break;

            case WAITING:
                startQuestTimer("beginning", null, null, Config.WAIT_TIME_VALAKAS);
                break;

            case FIGHTING:
                final int loc_x = info.getInteger("loc_x");
                final int loc_y = info.getInteger("loc_y");
                final int loc_z = info.getInteger("loc_z");
                final int heading = info.getInteger("heading");
                final int hp = info.getInteger("currentHP");
                final int mp = info.getInteger("currentMP");

                final Npc valakas = addSpawn(VALAKAS, loc_x, loc_y, loc_z, heading, false, 0, false);
                GrandBossManager.getInstance().addBoss((GrandBoss) valakas);

                valakas.getStatus().setHpMp(hp, mp);
                valakas.forceRunStance();

                // stores current time for inactivity task.
                _timeTracker = System.currentTimeMillis();

                // Start timers.
                startQuestTimerAtFixedRate("regen_task", valakas, null, 60000);
                startQuestTimerAtFixedRate("skill_task", valakas, null, 2000);
                break;
        }
    }

    @Override
    protected void registerNpcs() {
        addEventIds(VALAKAS, ScriptEventType.ON_ATTACK, ScriptEventType.ON_KILL, ScriptEventType.ON_SPAWN, ScriptEventType.ON_AGGRO);
    }

    @Override
    public String onTimer(String name, Npc npc, Player player) {
        if (name.equalsIgnoreCase("beginning")) {
            // Stores current time
            _timeTracker = System.currentTimeMillis();

            // Spawn Valakas and set him invul.
            npc = addSpawn(VALAKAS, 212852, -114842, -1632, 0, false, 0, false);
            GrandBossManager.getInstance().addBoss((GrandBoss) npc);
            npc.setInvul(true);

            // Sound + socialAction.
            for (Player plyr : VALAKAS_LAIR.getKnownTypeInside(Player.class)) {
                plyr.sendPacket(new PlaySound(1, "B03_A", npc));
                plyr.sendPacket(new SocialAction(npc, 3));
            }

            // Launch the cinematic, and tasks (regen + skill).
            startQuestTimer("spawn_1", npc, null, 2000); // 2000
            startQuestTimer("spawn_2", npc, null, 3500); // 1500
            startQuestTimer("spawn_3", npc, null, 6800); // 3300
            startQuestTimer("spawn_4", npc, null, 9700); // 2900
            startQuestTimer("spawn_5", npc, null, 12400); // 2700
            startQuestTimer("spawn_6", npc, null, 12401); // 1
            startQuestTimer("spawn_7", npc, null, 15601); // 3200
            startQuestTimer("spawn_8", npc, null, 17001); // 1400
            startQuestTimer("spawn_9", npc, null, 23701); // 6700 - end of cinematic
            startQuestTimer("spawn_10", npc, null, 29401); // 5700 - AI + unlock
        }
        // Regeneration && inactivity task
        else if (name.equalsIgnoreCase("regen_task")) {
            // Inactivity task - 15min
            if (GrandBossManager.getInstance().getBossStatus(VALAKAS) == FIGHTING) {
                if (_timeTracker + 900000 < System.currentTimeMillis()) {
                    // Set it dormant.
                    GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);

                    // Drop all players from the zone.
                    VALAKAS_LAIR.oustAllPlayers();

                    // Cancel skill_task and regen_task.
                    cancelQuestTimers("regen_task", npc);
                    cancelQuestTimers("skill_task", npc);

                    // Delete current instance of Valakas.
                    npc.deleteMe();

                    return null;
                }
            }

            // Regeneration buff.
            if (Rnd.get(30) == 0) {
                L2Skill skillRegen;
                final double hpRatio = npc.getStatus().getHpRatio();

                // Current HPs are inferior to 25% ; apply lvl 4 of regen skill.
                if (hpRatio < 0.25) {
                    skillRegen = SkillTable.getInstance().getInfo(4691, 4);
                }
                // Current HPs are inferior to 50% ; apply lvl 3 of regen skill.
                else if (hpRatio < 0.5) {
                    skillRegen = SkillTable.getInstance().getInfo(4691, 3);
                }
                // Current HPs are inferior to 75% ; apply lvl 2 of regen skill.
                else if (hpRatio < 0.75) {
                    skillRegen = SkillTable.getInstance().getInfo(4691, 2);
                } else {
                    skillRegen = SkillTable.getInstance().getInfo(4691, 1);
                }

                skillRegen.applyEffects(npc, npc);
            }
        }
        // Spawn cinematic, regen_task and choose of skill.
        else if (name.equalsIgnoreCase("spawn_1")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1800, 180, -1, 1500, 10000, 0, 0, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_2")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1300, 180, -5, 3000, 10000, 0, -5, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_3")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 500, 180, -8, 600, 10000, 0, 60, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_4")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 800, 180, -8, 2700, 10000, 0, 30, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_5")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 200, 250, 70, 0, 10000, 30, 80, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_6")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 250, 70, 2500, 10000, 30, 80, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_7")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 150, 30, 0, 10000, -10, 60, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_8")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1200, 150, 20, 2900, 10000, -10, 30, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_9")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 750, 170, -10, 3400, 4000, 10, -15, 1, 0));
        } else if (name.equalsIgnoreCase("spawn_10")) {
            GrandBossManager.getInstance().setBossStatus(VALAKAS, FIGHTING);
            npc.setInvul(false);

            startQuestTimerAtFixedRate("regen_task", npc, null, 60000);
            startQuestTimerAtFixedRate("skill_task", npc, null, 2000);
        }
        // Death cinematic, spawn of Teleport Cubes.
        else if (name.equalsIgnoreCase("die_1")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 2000, 130, -1, 0, 10000, 0, 0, 1, 1));
        } else if (name.equalsIgnoreCase("die_2")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 210, -5, 3000, 10000, -13, 0, 1, 1));
        } else if (name.equalsIgnoreCase("die_3")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1300, 200, -8, 3000, 10000, 0, 15, 1, 1));
        } else if (name.equalsIgnoreCase("die_4")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1000, 190, 0, 500, 10000, 0, 10, 1, 1));
        } else if (name.equalsIgnoreCase("die_5")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 120, 0, 2500, 10000, 12, 40, 1, 1));
        } else if (name.equalsIgnoreCase("die_6")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 20, 0, 700, 10000, 10, 10, 1, 1));
        } else if (name.equalsIgnoreCase("die_7")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 10, 0, 1000, 10000, 20, 70, 1, 1));
        } else if (name.equalsIgnoreCase("die_8")) {
            VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 10, 0, 300, 250, 20, -20, 1, 1));

            for (SpawnLocation loc : CUBE_LOC) {
                addSpawn(31759, loc, false, 900000, false);
            }

            startQuestTimer("remove_players", null, null, 900000);
        } else if (name.equalsIgnoreCase("skill_task")) {
            callSkillAI(npc);
        } else if (name.equalsIgnoreCase("valakas_unlock")) {
            GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);
        } else if (name.equalsIgnoreCase("remove_players")) {
            VALAKAS_LAIR.oustAllPlayers();
        }

        return super.onTimer(name, npc, player);
    }

    @Override
    public String onSpawn(Npc npc) {
        npc.disableCoreAi(true);
        return super.onSpawn(npc);
    }

    @Override
    public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill) {
        if (npc.isInvul()) {
            return null;
        }

        if (attacker instanceof Playable) {
            // Curses
            if (attacker.testCursesOnAttack(npc)) {
                return null;
            }

            // Refresh timer on every hit.
            _timeTracker = System.currentTimeMillis();
        }
        return super.onAttack(npc, attacker, damage, skill);
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        // Cancel skill_task and regen_task.
        cancelQuestTimers("regen_task", npc);
        cancelQuestTimers("skill_task", npc);

        // Launch death animation.
        VALAKAS_LAIR.broadcastPacket(new PlaySound(1, "B03_D", npc));

        startQuestTimer("die_1", npc, null, 300); // 300
        startQuestTimer("die_2", npc, null, 600); // 300
        startQuestTimer("die_3", npc, null, 3800); // 3200
        startQuestTimer("die_4", npc, null, 8200); // 4400
        startQuestTimer("die_5", npc, null, 8700); // 500
        startQuestTimer("die_6", npc, null, 13300); // 4600
        startQuestTimer("die_7", npc, null, 14000); // 700
        startQuestTimer("die_8", npc, null, 16500); // 2500

        GrandBossManager.getInstance().setBossStatus(VALAKAS, DEAD);

        long respawnTime = (long) Config.SPAWN_INTERVAL_VALAKAS + Rnd.get(-Config.RANDOM_SPAWN_TIME_VALAKAS, Config.RANDOM_SPAWN_TIME_VALAKAS);
        respawnTime *= 3600000;

        startQuestTimer("valakas_unlock", null, null, respawnTime);

        // also save the respawn time so that the info is maintained past reboots
        StatSet info = GrandBossManager.getInstance().getStatSet(VALAKAS);
        info.set("respawn_time", System.currentTimeMillis() + respawnTime);
        GrandBossManager.getInstance().setStatSet(VALAKAS, info);

        return super.onKill(npc, killer);
    }

    @Override
    public String onAggro(Npc npc, Player player, boolean isPet) {
        return null;
    }

    private void callSkillAI(Npc npc) {
        if (npc.isInvul()) {
            return;
        }

        // Pickup a target if no or dead victim. 10% luck he decides to reconsiders his target.
        if (_actualVictim == null || _actualVictim.isDead() || !npc.knows(_actualVictim) || Rnd.get(10) == 0) {
            _actualVictim = getRandomPlayer(npc);
        }

        // If result is still null, Valakas will roam. Don't go deeper in skill AI.
        if (_actualVictim == null) {
            if (Rnd.get(10) == 0) {
                npc.moveUsingRandomOffset(1400);
            }

            return;
        }

        npc.getAI().tryToCast(_actualVictim, getRandomSkill(npc), 1);
    }

    /**
     * Pick a random skill.<br> Valakas will mostly use utility skills. If Valakas feels surrounded, he will use AoE
     * skills.<br> Lower than 50% HPs, he will begin to use Meteor skill.
     *
     * @param npc valakas
     * @return a usable skillId
     */
    private static int getRandomSkill(Npc npc) {
        final double hpRatio = npc.getStatus().getHpRatio();

        // Valakas Lava Skin is prioritary.
        if (hpRatio < 0.25 && Rnd.get(1500) == 0 && npc.getFirstEffect(4680) == null) {
            return LAVA_SKIN;
        }

        if (hpRatio < 0.5 && Rnd.get(60) == 0) {
            return METEOR_SWARM;
        }

        // Find enemies surrounding Valakas.
        final int[] playersAround = getPlayersCountInPositions(1200, npc, false);

        // Behind position got more ppl than front position, use behind aura skill.
        if (playersAround[1] > playersAround[0]) {
            return Rnd.get(BEHIND_SKILLS);
        }

        // Use front aura skill.
        return Rnd.get(FRONT_SKILLS);
    }
}