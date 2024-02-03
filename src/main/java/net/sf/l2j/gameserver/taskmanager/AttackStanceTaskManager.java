package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.events.OnAttackStanceEnd;
import net.sf.l2j.gameserver.events.OnAttackStanceStart;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns off attack stance of {@link Creature} after ATTACK_STANCE_PERIOD (set to 15sec by default).
 */
public final class AttackStanceTaskManager implements Runnable {
    private static final long ATTACK_STANCE_PERIOD = 15000; // 15 seconds

    private final Map<Creature, Long> _creatures = new ConcurrentHashMap<>();

    private AttackStanceTaskManager() {
        // Run task each second.
        ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
    }

    @Override
    public void run() {
        // List is empty, skip.
        if (_creatures.isEmpty()) {
            return;
        }

        // Get current time.
        final long time = System.currentTimeMillis();

        // Loop all characters.
        for (Map.Entry<Creature, Long> entry : _creatures.entrySet()) {
            // Time hasn't passed yet, skip.
            if (time < entry.getValue()) {
                continue;
            }

            // Get character.
            final Creature creature = entry.getKey();

            // Stop character attack stance animation.
            creature.broadcastPacket(new AutoAttackStop(creature.getObjectId()));

            if (creature instanceof Player) {
                // Stop summon attack stance animation.
                final Summon summon = ((Player) creature).getSummon();
                if (summon != null) {
                    summon.broadcastPacket(new AutoAttackStop(summon.getObjectId()));
                }
            }

            // Remove task.
            _creatures.remove(creature);

            creature.getEventListener().notify(new OnAttackStanceEnd(creature));
        }
    }

    /**
     * Add a {@link Creature} to the {@link AttackStanceTaskManager}.
     *
     * @param creature : The Creature to add.
     */
    public void add(Creature creature) {
        if (creature instanceof Playable) {
            for (Cubic cubic : creature.getActingPlayer().getCubicList()) {
                if (cubic.getId() != Cubic.LIFE_CUBIC) {
                    cubic.doAction();
                }
            }
        }

        long timestamp = System.currentTimeMillis() + ATTACK_STANCE_PERIOD;
        _creatures.put(creature, System.currentTimeMillis() + ATTACK_STANCE_PERIOD);
        creature.getEventListener().notify(new OnAttackStanceStart(creature, timestamp));
    }

    /**
     * @param creature : The Creature to remove.
     * @return true if the {@link Creature} was successfully dropped from the {@link AttackStanceTaskManager}.
     */
    public boolean remove(Creature creature) {
        if (creature instanceof Summon) {
            creature = creature.getActingPlayer();
        }

        return _creatures.remove(creature) != null;
    }

    /**
     * @param creature : The Creature to test.
     * @return true if a {@link Creature} is registered in the {@link AttackStanceTaskManager}, false otherwise.
     */
    public final boolean isInAttackStance(Creature creature) {
        if (creature instanceof Summon) {
            creature = creature.getActingPlayer();
        }

        return _creatures.containsKey(creature);
    }

    public static final AttackStanceTaskManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final AttackStanceTaskManager INSTANCE = new AttackStanceTaskManager();
    }
}