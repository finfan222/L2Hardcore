package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.enums.ScriptEventType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * Gordon behavior. This boss attacks cursed weapons holders at sight.<br> When he isn't attacking, he follows a
 * pre-established path around Goddard castle.
 */
public class Gordon extends AttackableAIScript {
    private static final int GORDON = 29095;

    private static final Location[] LOCS =
        {
            new Location(141569, -45908, -2387),
            new Location(142494, -45456, -2397),
            new Location(142922, -44561, -2395),
            new Location(143672, -44130, -2398),
            new Location(144557, -43378, -2325),
            new Location(145839, -43267, -2301),
            new Location(147044, -43601, -2307),
            new Location(148140, -43206, -2303),
            new Location(148815, -43434, -2328),
            new Location(149862, -44151, -2558),
            new Location(151037, -44197, -2708),
            new Location(152555, -42756, -2836),
            new Location(154808, -39546, -3236),
            new Location(155333, -39962, -3272),
            new Location(156531, -41240, -3470),
            new Location(156863, -43232, -3707),
            new Location(156783, -44198, -3764),
            new Location(158169, -45163, -3541),
            new Location(158952, -45479, -3473),
            new Location(160039, -46514, -3634),
            new Location(160244, -47429, -3656),
            new Location(159155, -48109, -3665),
            new Location(159558, -51027, -3523),
            new Location(159396, -53362, -3244),
            new Location(160872, -56556, -2789),
            new Location(160857, -59072, -2613),
            new Location(160410, -59888, -2647),
            new Location(158770, -60173, -2673),
            new Location(156368, -59557, -2638),
            new Location(155188, -59868, -2642),
            new Location(154118, -60591, -2731),
            new Location(153571, -61567, -2821),
            new Location(153457, -62819, -2886),
            new Location(152939, -63778, -3003),
            new Location(151816, -64209, -3120),
            new Location(147655, -64826, -3433),
            new Location(145422, -64576, -3369),
            new Location(144097, -64320, -3404),
            new Location(140780, -61618, -3096),
            new Location(139688, -61450, -3062),
            new Location(138267, -61743, -3056),
            new Location(138613, -58491, -3465),
            new Location(138139, -57252, -3517),
            new Location(139555, -56044, -3310),
            new Location(139107, -54537, -3240),
            new Location(139279, -53781, -3091),
            new Location(139810, -52687, -2866),
            new Location(139657, -52041, -2793),
            new Location(139215, -51355, -2698),
            new Location(139334, -50514, -2594),
            new Location(139817, -49715, -2449),
            new Location(139824, -48976, -2263),
            new Location(140130, -47578, -2213),
            new Location(140483, -46339, -2382),
            new Location(141569, -45908, -2387)
        };

    // The current Location node index.
    private static int _currentNode;

    public Gordon() {
        super("ai/boss");

        final Npc npc = findSpawn(GORDON);
        if (npc != null) {
            startQuestTimerAtFixedRate("ai_loop", npc, null, 1000);
        }
    }

    @Override
    protected void registerNpcs() {
        addEventIds(GORDON, ScriptEventType.ON_KILL, ScriptEventType.ON_SPAWN);
    }

    @Override
    public String onTimer(String name, Npc npc, Player player) {
        if (name.equalsIgnoreCase("ai_loop")) {
            final Attackable gordon = (Attackable) npc;

            // Doesn't bother about task AI if the NPC is already fighting.
            if (AttackStanceTaskManager.getInstance().isInAttackStance(gordon)) {
                return null;
            }

            // Check if player have Cursed Weapon and is in radius.
            for (Player pc : npc.getKnownTypeInRadius(Player.class, 5000)) {
                if (pc.isCursedWeaponEquipped()) {
                    gordon.forceAttack(pc, 200);
                    return null;
                }
            }

            // Test the NPC position and move on new position if current position is reached.
            final Location currentNode = LOCS[_currentNode];
            if (gordon.isIn3DRadius(currentNode, 100)) {
                // Update current node ; if the whole route is done, come back to point 0.
                _currentNode++;
                if (_currentNode >= LOCS.length) {
                    _currentNode = 0;
                }

                gordon.forceWalkStance();
                gordon.getAI().tryToMoveTo(LOCS[_currentNode], null);
            } else if (gordon.getCast().isCastingNow()) {
                gordon.forceWalkStance();
                gordon.getAI().tryToMoveTo(LOCS[_currentNode], null);
            } else if (!gordon.isDead() && !gordon.isMoving() && !gordon.getCast().isCastingNow()) {
                // This means Gordon has been drawn off of this path by combat and should now go back to his path
                // TODO find nearest path point and go to that?
                gordon.forceWalkStance();
                gordon.teleportTo(LOCS[_currentNode], 0);
            }
        }
        return super.onTimer(name, npc, player);
    }

    @Override
    public String onSpawn(Npc npc) {
        // Initialize current node.
        _currentNode = 0;

        // Launch the AI loop.
        startQuestTimerAtFixedRate("ai_loop", npc, null, 1000);

        return super.onSpawn(npc);
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        cancelQuestTimers("ai_loop", npc);

        return super.onKill(npc, killer);
    }

    private static Npc findSpawn(int npcId) {
        for (Spawn spawn : SpawnTable.getInstance().getSpawns()) {
            if (spawn.getNpcId() == npcId) {
                return spawn.getNpc();
            }
        }
        return null;
    }
}