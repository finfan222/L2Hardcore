package net.sf.l2j.gameserver.taskmanager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.NpcRace;
import net.sf.l2j.gameserver.events.OnDayCycleChange;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@Slf4j
public class AmbushTaskManager implements Runnable {

    private static final String[][] AMBUSH_TEXT = {
        // UNKNOWN empty
        {},
        // UNDEAD
        {
            "Живые здесь?! Присоединяйся к нам $name, мертвые не знают проблем!",
            "Убирайся! Этот путь был проложен мертвыми и мертвые - хранят его $name!",
            "Блуждание во тьме приводит к собственной могиле $name!",
            "СДОХНИ-СДОХНИ-СДОХНИ $name!",
            "Каждые имеет право на смерть, подойди, я познакомлю тебя с ней...",
            "Ээ-э-э-э... не дви-гай-ся... т-ы-м-мойййй..."
        },
        // MAGICCREATURE
        {
            "$name мерзкое создание, как ты посмело оказаться здесь?!",
            "Уничтожение таких как ты $name - это залог жизни всех нас, существ сотканных из магии!",
            "$name что ты можешь противопоставить существу из магии!? НИЧЕГО!",
            "Вы только и можете уничтожать! Сейчас, уничтожению подвергнетесь и вы сами!!!"
        },
        // BEAST
        {
            "*Свирепый рев*!",
            "*Недовольное рычание*!",
            "*Неожиданная агрессия*!"
        },
        // ANIMAL
        {
            "*Свирепый рев*!",
            "*Недовольное рычание*!",
            "*Неожиданная агрессия*!"
        },
        // PLANT
        {
            "*Пугающий шелест листвы*!",
            "*Агрессивные постукивания ветвями*!",
            "*Ужасные трески коры*!"
        },
        // HUMANOID
        {
            "Кто тут у нас? Адена есть? Му-ха-ха-ха!",
            "Оу, кажется у нас нежданные гости. Покажу вам $name свое гостеприимство.",
            "!",
            "Анекдот, угадай \"кто выходит ночью из города?\"... ответ - Самоубийца! Ах-ха-ха-ха!",
            "Я знаю, я знаю! Вы охотитесь за мной! Но первый шаг сделаю я! Защищайся ублюдок!"
        },
        // SPIRIT
        {
            "*Холодное веяние исходит от Spirit*!",
            "*Spirit молча движется к вам*!",
            "*...*!"
        },
        // ANGEL
        {
            "Quomodo audes esse in regione Seraphim?",
            "Quis te huc intromisit? Discedite!",
            "Haec est terra sancta deae Einhasad. Te praesentia matrem omnium offendit!"
        },
        // DEMON
        {
            "A-rul shach kigon!",
            "Anach kyree! Galtak Ered'nash!",
            "Bar shahur..."
        },
        // DRAGON
        {},
        // GIANT
        {},
        // BUG
        {},
        // FAIRIE
        {},
        // HUMAN
        {},
        // ELVE
        {},
        // DARKELVE
        {},
        // ORC
        {},
        // DWARVE
        {}
    };

    @Getter(lazy = true)
    private static final AmbushTaskManager instance = new AmbushTaskManager();

    private ScheduledFuture<?> ambushTask;

    private AmbushTaskManager() {
        GlobalEventListener.register(OnDayCycleChange.class).forEach(this::onDayCycleChange);
        if (DayNightTaskManager.getInstance().is(DayCycle.NIGHT)) {
            start();
        }
        log.info("Loaded successfully!");
    }

    public void start() {
        ambushTask = ThreadPool.scheduleAtFixedRate(this, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
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
            .filter(player -> !player.isDead())
            .filter(player -> !(player.isInsideZone(ZoneId.PEACE)
                || player.isInsideZone(ZoneId.TOWN)
                || player.isInsideZone(ZoneId.PVP)
                || player.isInsideZone(ZoneId.SIEGE))
            ).forEach(this::ambushAttack);
    }

    private void ambushAttack(Player player) {
        List<Monster> list = player.getKnownTypeInRadius(Monster.class, 1500).stream()
            .filter(monster -> Rnd.calcChance(66, 100))
            .filter(monster -> !monster.isRaidBoss() && !monster.isRaidRelated() && !(monster instanceof Chest))
            .filter(monster -> !monster.isDead() && !monster.getAttack().isAttackingNow())
            .toList();

        if (!list.isEmpty()) {
            player.sendMessage("Засада! Вас окружают монстры со всех сторон!");
            player.sendPacket(new PlaySound("ItemSound3.sys_siege_start"));
            for (Monster monster : list) {
                NpcRace race = monster.getTemplate().getRace();
                switch (race) {
                    case OTHER, NONLIVING, SIEGEWEAPON, MERCENARIE, DEFENDINGARMY:
                        break;

                    default:
                        if (Rnd.calcChance(10, 100)) {
                            if (AMBUSH_TEXT[race.ordinal()].length > 0) {
                                String text = Rnd.get(AMBUSH_TEXT[race.ordinal()]);
                                monster.broadcastNpcSay(text.replace("$name", player.getName()));
                            }
                        }
                        break;
                }

                float nightBonus = Rnd.get(1, 100) / 100f + 1f;
                log.info("{} has night bonus={}", monster.getName(), nightBonus);
                monster.setNightExpSpBonus(nightBonus);
                monster.setWalkOrRun(true);
                monster.setTarget(player);
                monster.getAggroList().addDamageHate(player, 0, Rnd.get(999, 9999));
                monster.getAI().tryToAttack(player);
            }
        }
    }

    private void onDayCycleChange(OnDayCycleChange event) {
        if (event.getCurrent() == DayCycle.NIGHT) {
            start();
        } else if (event.getPrevious() == DayCycle.NIGHT) {
            stop();
        }
    }
}
