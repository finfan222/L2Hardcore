package net.sf.l2j.gameserver.data.manager;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.events.OnDayCycleChange;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.Spawn;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DayNightManager {

    private final List<Spawn> _dayCreatures = new ArrayList<>();
    private final List<Spawn> _nightCreatures = new ArrayList<>();

    protected DayNightManager() {
        GlobalEventListener.register(OnDayCycleChange.class).forEach(this::onDayCycleChange);
    }

    public void addDayCreature(Spawn spawnDat) {
        _dayCreatures.add(spawnDat);
    }

    public void addNightCreature(Spawn spawnDat) {
        _nightCreatures.add(spawnDat);
    }

    private void onDayCycleChange(OnDayCycleChange event) {
        if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty()) {
            return;
        }

        spawnCreatures(event.getCurrent() == DayCycle.NIGHT);
    }

    public void spawnCreatures(boolean isNight) {
        final List<Spawn> creaturesToUnspawn = (isNight) ? _dayCreatures : _nightCreatures;
        final List<Spawn> creaturesToSpawn = (isNight) ? _nightCreatures : _dayCreatures;

        for (Spawn spawn : creaturesToUnspawn) {
            spawn.setRespawnState(false);

            final Npc last = spawn.getNpc();
            if (last != null) {
                last.deleteMe();
            }
        }

        for (Spawn spawn : creaturesToSpawn) {
            spawn.setRespawnState(true);
            spawn.doSpawn(false);
        }

        log.info("Loaded {} creatures spawns.", ((isNight) ? "night" : "day"));
    }

    public void cleanUp() {
        _nightCreatures.clear();
        _dayCreatures.clear();
    }

    public static DayNightManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final DayNightManager INSTANCE = new DayNightManager();
    }
}