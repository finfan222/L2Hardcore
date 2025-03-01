package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Destroys {@link Creature} corpse after specified time.
 */
public final class DecayTaskManager implements Runnable {
    private final Map<Creature, Long> _creatures = new ConcurrentHashMap<>();

    protected DecayTaskManager() {
        // Run task each second.
        ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
    }

    @Override
    public final void run() {
        // List is empty, skip.
        if (_creatures.isEmpty()) {
            return;
        }

        // Get current time.
        final long time = System.currentTimeMillis();

        // Loop all characters.
        for (Map.Entry<Creature, Long> entry : _creatures.entrySet()) {
            final Creature creature = entry.getKey();

            // If decayed creature is a Summon, check if he is still linked to its owner. If not, decay task is canceled.
            if (creature instanceof Summon && ((Summon) creature).getOwner().getSummon() != creature) {
                _creatures.remove(creature);
                continue;
            }

            // Time hasn't passed yet, skip.
            if (time < entry.getValue()) {
                continue;
            }

            // Decay the Creature.
            creature.onDecay();

            // Remove the entry.
            _creatures.remove(creature);
        }
    }

    /**
     * Adds a {@link Creature} to the {@link DecayTaskManager} with additional interval.
     *
     * @param creature : The {@link Creature} to be added.
     * @param interval : Interval in seconds, after which the decay task is triggered.
     */
    public final void add(Creature creature, int interval) {
        // If character is a Monster.
        if (creature instanceof Monster) {
            final Monster monster = ((Monster) creature);

            // If Monster is spoiled or seeded, double the corpse delay.
            if (monster.getSpoilState().isSpoiled() || monster.getSeedState().isSeeded()) {
                interval *= 2;
            }
        }

        _creatures.put(creature, System.currentTimeMillis() + interval * 1000);
    }

    /**
     * Removes the {@link Creature} passed as parameter from the {@link DecayTaskManager}.
     *
     * @param creature : The {@link Creature} to be removed.
     */
    public final void cancel(Creature creature) {
        _creatures.remove(creature);
    }

    /**
     * Removes the {@link Monster} passed as parameter from the {@link DecayTaskManager}.
     *
     * @param monster : The {@link Monster} to be tested.
     * @return boolean : True, when action can be applied on a corpse.
     */
    public final boolean isCorpseActionAllowed(Monster monster) {
        // Verify if a corpse exists.
        Long time = _creatures.get(monster);
        if (time == null) {
            return false;
        }

        // Get corpse action interval, which is half the corpse decay.
        int corpseTime = monster.getTemplate().getCorpseTime() * 1000 / 2;

        // If the Monster is spoiled or seeded, double the corpse action interval.
        if (monster.getSpoilState().isSpoiled() || monster.getSeedState().isSeeded()) {
            corpseTime *= 2;
        }

        // Check last corpse action time.
        return System.currentTimeMillis() < time - corpseTime;
    }

    public static final DecayTaskManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final class SingletonHolder {
        protected static final DecayTaskManager INSTANCE = new DecayTaskManager();
    }
}