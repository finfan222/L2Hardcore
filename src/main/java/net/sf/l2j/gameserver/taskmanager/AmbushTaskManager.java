package net.sf.l2j.gameserver.taskmanager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.events.OnDayCycleChange;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@Slf4j
public class AmbushTaskManager implements Runnable {

    @Getter(lazy = true)
    private static final AmbushTaskManager instance = new AmbushTaskManager();

    private ScheduledFuture<?> ambushTask;

    private AmbushTaskManager() {
        GlobalEventListener.register(OnDayCycleChange.class).forEach(this::onDayCycleChange);
    }

    public void start() {
        ambushTask = ThreadPool.schedule(this, TimeUnit.MINUTES.toMillis(Rnd.get(5, 60)));
    }

    private void stop() {
        if (ambushTask != null) {
            ambushTask.cancel(false);
            ambushTask = null;
        }
    }

    @Override
    public void run() {
        World.getInstance().getPlayers().stream()
            .filter(player -> !(player.isInsideZone(ZoneId.PEACE)
                || player.isInsideZone(ZoneId.TOWN)
                || player.isInsideZone(ZoneId.PVP)
                || player.isInsideZone(ZoneId.SIEGE))
            ).forEach(player -> {
                WorldRegion region = player.getRegion();
                List<WorldRegion> surroundingRegions = region.getSurroundingRegions();
                for (WorldRegion nextRegion : surroundingRegions) {
                    List<WorldObject> monsters = region.getObjects().stream()
                        .filter(obj -> obj instanceof Monster monster && !monster.isRaidBoss() && !monster.isRaidRelated())
                        .toList();
                }
            });
    }

    /*
        UNKNOWN(null, null),
        UNDEAD(null, null),
        MAGICCREATURE(Stats.PATK_MCREATURES, Stats.PDEF_MCREATURES),
        BEAST(Stats.PATK_MONSTERS, Stats.PDEF_MONSTERS),
        ANIMAL(Stats.PATK_ANIMALS, Stats.PDEF_ANIMALS),
        PLANT(Stats.PATK_PLANTS, Stats.PDEF_PLANTS),
        HUMANOID(null, null),
        SPIRIT(null, null),
        ANGEL(null, null),
        DEMON(null, null),
        DRAGON(Stats.PATK_DRAGONS, Stats.PDEF_DRAGONS),
        GIANT(Stats.PATK_GIANTS, Stats.PDEF_GIANTS),
        BUG(Stats.PATK_INSECTS, Stats.PDEF_INSECTS),
        FAIRIE(null, null),
        HUMAN(null, null),
        ELVE(null, null),
        DARKELVE(null, null),
        ORC(null, null),
        DWARVE(null, null),
        OTHER(null, null),
        NONLIVING(null, null),
        SIEGEWEAPON(null, null),
        DEFENDINGARMY(null, null),
        MERCENARIE(null, null);
     */
    private static String[][] AMBUSH_TEXT = {
        {},
        {
            "Живые здесь?! Присоединяйся к нам $name, мертвые не знают проблем!",
            "Убирайся! Этот путь был проложен мертвыми и мертвые - хранят его $name!",
            "Блуждание во тьме приводит к собственной могиле $name!",
            "СДОХНИ-СДОХНИ-СДОХНИ $name!"
        },
        {},
        {},
        {},
    };

    private void ambushAttack(WorldRegion region, Player player) {
        if (!player.getAppearance().isVisible() || player.isAffected(EffectFlag.SILENT_MOVE)) {
            return;
        }

        region.getObjects().stream()
            .filter(obj -> obj instanceof Monster monster && !monster.isRaidBoss() && !monster.isRaidRelated())
            .filter(monster -> Rnd.calcChance(20, 100))
            .map(obj -> (Monster) obj)
            .forEach(monster -> {
                switch (monster.getTemplate().getRace()) {
                    case HUMANOID, DARKELVE, HUMAN, ORC, ELVE, DWARVE -> {

                    }
                }
                monster.getAggroList().addDamageHate(player, 0, Rnd.get(999, 9999));
            });
    }

    private void onDayCycleChange(OnDayCycleChange event) {
        if (event.getCurrent() == DayCycle.NIGHT) {
            start();
        } else if (event.getPrevious() == DayCycle.NIGHT) {
            stop();
        }
    }
}
