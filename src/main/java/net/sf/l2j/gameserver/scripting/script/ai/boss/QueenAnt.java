package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.Config;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.skills.ElementType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

public class QueenAnt extends AttackableAIScript {
    private static final BossZone ZONE = ZoneManager.getInstance().getZoneById(110017, BossZone.class);

    private static final int QUEEN = 29001;
    private static final int LARVA = 29002;
    private static final int NURSE = 29003;
    private static final int GUARD = 29004;
    private static final int ROYAL = 29005;

    private static final Location[] PLAYER_TELE_OUT =
        {
            new Location(-19480, 187344, -5600),
            new Location(-17928, 180912, -5520),
            new Location(-23808, 182368, -5600)
        };

    private static final byte ALIVE = 0;
    private static final byte DEAD = 1;

    private Monster _larva = null;

    public QueenAnt() {
        super("ai/boss");

        // Queen Ant is dead, calculate the respawn time. If passed, we spawn it directly, otherwise we set a task to spawn it lately.
        if (GrandBossManager.getInstance().getBossStatus(QUEEN) == DEAD) {
            final long temp = GrandBossManager.getInstance().getStatSet(QUEEN).getLong("respawn_time") - System.currentTimeMillis();
            if (temp > 0) {
                startQuestTimer("queen_unlock", null, null, temp);
            } else {
                spawnBoss(true);
            }
        }
        // Queen Ant is alive, spawn it using stored data.
        else {
            spawnBoss(false);
        }
    }

    @Override
    protected void registerNpcs() {
        addAttackId(QUEEN, LARVA, NURSE, GUARD, ROYAL);
        addAggroRangeEnterId(LARVA, NURSE, GUARD, ROYAL);
        addFactionCallId(QUEEN, NURSE);
        addKillId(QUEEN, NURSE, ROYAL);
        addSkillSeeId(QUEEN, LARVA, NURSE, GUARD, ROYAL);
        addSpawnId(LARVA, NURSE);
        addExitZoneId(110017);
    }

    @Override
    public String onTimer(String name, Npc npc, Player player) {
        if (name.equalsIgnoreCase("action")) {
            // Animation timer.
            if (Rnd.get(10) < 3) {
                npc.broadcastPacket(new SocialAction(npc, (Rnd.nextBoolean()) ? 3 : 4));
            }

            // Teleport Royal Guards back in zone if out.
            ((Monster) npc).getMinionList().getSpawnedMinions().stream().filter(m -> m.getNpcId() == ROYAL && !ZONE.isInsideZone(m)).forEach(Monster::teleportToMaster);
        } else if (name.equalsIgnoreCase("chaos")) {
            // Randomize the target for Royal Guards.
            ((Monster) npc).getMinionList().getSpawnedMinions().stream().filter(m -> m.getNpcId() == ROYAL && m.isInCombat() && Rnd.get(100) < 66).forEach(m -> m.getAggroList().randomizeAttack());

            // Relaunch a new chaos task.
            startQuestTimer("chaos", npc, null, 90000L + Rnd.get(240000));
        } else if (name.equalsIgnoreCase("clean")) {
            // Delete the larva and the reference.
            _larva.deleteMe();
            _larva = null;
        } else if (name.equalsIgnoreCase("queen_unlock")) {
            // Choose a teleport location, and teleport players out of Queen Ant zone.
            if (Rnd.get(100) < 33) {
                ZONE.movePlayersTo(PLAYER_TELE_OUT[0]);
            } else if (Rnd.nextBoolean()) {
                ZONE.movePlayersTo(PLAYER_TELE_OUT[1]);
            } else {
                ZONE.movePlayersTo(PLAYER_TELE_OUT[2]);
            }

            // Spawn the boss.
            spawnBoss(true);
        }
        return super.onTimer(name, npc, player);
    }

    @Override
    public String onAggro(Npc npc, Player player, boolean isPet) {
        final Playable realBypasser = (isPet && player.getSummon() != null) ? player.getSummon() : player;
        if (realBypasser.testCursesOnAggro(npc)) {
            return null;
        }

        return super.onAggro(npc, player, isPet);
    }

    @Override
    public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill) {
        if (attacker instanceof Playable) {
            // Curses
            if (attacker.testCursesOnAttack(npc, QUEEN)) {
                return null;
            }

            // Pick current attacker, and make actions based on it and the actual distance range seperating them.
            if (npc.getNpcId() == QUEEN) {
                if (skill != null && skill.getElement() == ElementType.FIRE && Rnd.get(100) < 70) {
                    npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
                } else {
                    final double dist = npc.distance3D(attacker);
                    if (dist > 500 && Rnd.get(100) < 10) {
                        npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
                    } else if (dist > 150 && Rnd.get(100) < 10) {
                        npc.getAI().tryToCast(attacker, (Rnd.get(10) < 8) ? FrequentSkill.QUEEN_ANT_STRIKE.getSkill() : FrequentSkill.QUEEN_ANT_SPRINKLE.getSkill());
                    } else if (dist < 250 && Rnd.get(100) < 5) {
                        npc.getAI().tryToCast(attacker, FrequentSkill.QUEEN_ANT_BRANDISH.getSkill());
                    }
                }
            }
        }
        return super.onAttack(npc, attacker, damage, skill);
    }

    @Override
    public String onExitZone(Creature character, ZoneType zone) {
        if (character instanceof GrandBoss) {
            final GrandBoss queen = (GrandBoss) character;
            if (queen.getNpcId() == QUEEN) {
                queen.teleportTo(-21610, 181594, -5734, 0);
            }
        }
        return super.onExitZone(character, zone);
    }

    @Override
    public String onFactionCall(Attackable caller, Attackable called, Creature target) {
        switch (called.getNpcId()) {
            case QUEEN:
                final double dist = called.distance3D(target);
                if (dist > 500 && Rnd.get(100) < 3) {
                    called.getAI().tryToCast(target, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
                } else if (dist > 150 && Rnd.get(100) < 3) {
                    called.getAI().tryToCast(target, (Rnd.get(10) < 8) ? FrequentSkill.QUEEN_ANT_STRIKE.getSkill() : FrequentSkill.QUEEN_ANT_SPRINKLE.getSkill());
                } else if (dist < 250 && Rnd.get(100) < 2) {
                    called.getAI().tryToCast(target, FrequentSkill.QUEEN_ANT_BRANDISH.getSkill());
                }
                break;

            case NURSE:
                // If the faction caller is the larva, assist it directly, no matter what.
                if (caller.getNpcId() == LARVA) {
                    called.getAI().tryToCast(caller, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
                }
                // If the faction caller is Queen Ant, then check first Larva.
                else if (caller.getNpcId() == QUEEN) {
                    if (_larva != null && _larva.getStatus().getHpRatio() < 1.0) {
                        called.getAI().tryToCast(_larva, Rnd.nextBoolean() ? FrequentSkill.NURSE_HEAL_1.getSkill() : FrequentSkill.NURSE_HEAL_2.getSkill());
                    } else {
                        called.getAI().tryToCast(caller, FrequentSkill.NURSE_HEAL_1.getSkill());
                    }
                }
                break;
        }
        return null;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        if (npc.getNpcId() == QUEEN) {
            // Broadcast death sound.
            npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));

            // Flag Queen Ant as dead.
            GrandBossManager.getInstance().setBossStatus(QUEEN, DEAD);

            // Calculate the next respawn time.
            final long respawnTime = (long) (Config.SPAWN_INTERVAL_AQ + Rnd.get(-Config.RANDOM_SPAWN_TIME_AQ, Config.RANDOM_SPAWN_TIME_AQ)) * 3600000;

            // Cancel tasks.
            cancelQuestTimers("action");
            cancelQuestTimers("chaos");

            // Start respawn timer, and clean the monster references.
            startQuestTimer("queen_unlock", null, null, respawnTime);
            startQuestTimer("clean", null, null, 5000);

            // Save the respawn time so that the info is maintained past reboots
            final StatSet info = GrandBossManager.getInstance().getStatSet(QUEEN);
            info.set("respawn_time", System.currentTimeMillis() + respawnTime);
            GrandBossManager.getInstance().setStatSet(QUEEN, info);
        } else {
            // Set the respawn time of Royal Guards and Nurses. Pick the npc master.
            final Monster minion = ((Monster) npc);
            final Monster master = minion.getMaster();

            if (master != null && master.hasMinions()) {
                master.getMinionList().onMinionDie(minion, (npc.getNpcId() == NURSE) ? 10000 : (280000 + (Rnd.get(40) * 1000)));
            }

            return null;
        }
        return super.onKill(npc, killer);
    }

    @Override
    public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet) {
        final Playable realAttacker = (isPet && caster.getSummon() != null) ? caster.getSummon() : caster;
        if (!Config.RAID_DISABLE_CURSE && realAttacker.getStatus().getLevel() - npc.getStatus().getLevel() > 8) {
            final L2Skill curse = FrequentSkill.RAID_CURSE.getSkill();

            npc.broadcastPacket(new MagicSkillUse(npc, realAttacker, curse.getId(), curse.getLevel(), 300, 0));
            curse.applyEffects(npc, realAttacker);

            ((Attackable) npc).getAggroList().stopHate(realAttacker);
            return null;
        }

        // If Queen Ant see an aggroable skill, try to launch Queen Ant Strike.
        if (npc.getNpcId() == QUEEN && skill.getAggroPoints() > 0 && Rnd.get(100) < 15) {
            npc.getAI().tryToCast(realAttacker, FrequentSkill.QUEEN_ANT_STRIKE.getSkill());
        }

        return super.onSkillSee(npc, caster, skill, targets, isPet);
    }

    @Override
    public String onSpawn(Npc npc) {
        switch (npc.getNpcId()) {
            case LARVA:
                npc.setMortal(false);
                npc.setIsImmobilized(true);
                npc.disableCoreAi(true);
                break;
            case NURSE:
                npc.disableCoreAi(true);
                break;
        }
        return super.onSpawn(npc);
    }

    /**
     * Make additional actions on boss spawn : register the NPC as boss, activate tasks, spawn the larva.
     *
     * @param freshStart : If true, it uses static data, otherwise it uses stored data.
     */
    private void spawnBoss(boolean freshStart) {
        final GrandBoss queen;
        if (freshStart) {
            GrandBossManager.getInstance().setBossStatus(QUEEN, ALIVE);

            queen = (GrandBoss) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0, false);
        } else {
            final StatSet info = GrandBossManager.getInstance().getStatSet(QUEEN);

            queen = (GrandBoss) addSpawn(QUEEN, info.getInteger("loc_x"), info.getInteger("loc_y"), info.getInteger("loc_z"), info.getInteger("heading"), false, 0, false);
            queen.getStatus().setHpMp(info.getInteger("currentHP"), info.getInteger("currentMP"));
        }

        GrandBossManager.getInstance().addBoss(queen);

        startQuestTimerAtFixedRate("action", queen, null, 10000);
        startQuestTimer("chaos", queen, null, 90000L + Rnd.get(240000));

        queen.broadcastPacket(new PlaySound(1, "BS01_A", queen));

        _larva = (Monster) addSpawn(LARVA, -21600, 179482, -5846, Rnd.get(360), false, 0, false);
    }
}