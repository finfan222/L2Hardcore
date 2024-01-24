package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.DayNightManager;
import net.sf.l2j.gameserver.events.OnChangeDayCycle;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls game time, informs spawn manager about day/night spawns and players about daytime change. Informs players
 * about their extended activity in game.
 */
public class GameTimeTaskManager implements Runnable {

    public static int HOURS_PER_GAME_DAY = 4; // 4h is 1 game day
    public static int MINUTES_PER_GAME_DAY = HOURS_PER_GAME_DAY * 60; // 240m is 1 game day
    public static int SECONDS_PER_GAME_DAY = MINUTES_PER_GAME_DAY * 60; // 14400s is 1 game day

    private static final int MINUTES_PER_DAY = 24 * 60; // 24h * 60m
    private static final int TAKE_BREAK_HOURS = 2; // each 2h
    private static final int TAKE_BREAK_GAME_MINUTES = TAKE_BREAK_HOURS * MINUTES_PER_DAY / HOURS_PER_GAME_DAY; // 2h of real time is 720 game minutes
    private static final int MILLISECONDS_PER_GAME_MINUTE = SECONDS_PER_GAME_DAY / (MINUTES_PER_DAY) * 1000; // 10000ms is 1 game minute

    private final Map<Player, Integer> players = new ConcurrentHashMap<>();
    private final List<Quest> quests = new ArrayList<>();

    private int time;
    protected boolean isNight;

    protected GameTimeTaskManager() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        time = (int) (System.currentTimeMillis() - cal.getTimeInMillis()) / MILLISECONDS_PER_GAME_MINUTE;
        isNight = isNight();

        // Run task each 10 seconds.
        ThreadPool.scheduleAtFixedRate(this, MILLISECONDS_PER_GAME_MINUTE, MILLISECONDS_PER_GAME_MINUTE);
    }

    @Override
    public void run() {
        // Tick time.
        time++;

        // Quest listener.
        int gameTime = getGameTime();
        for (Quest quest : quests) {
            quest.onGameTime(gameTime);
        }

        // Shadow Sense skill, if set then perform day/night info.
        L2Skill skill = null;

        // Day/night has changed.
        if (isNight != isNight()) {
            // Change day/night.
            isNight = !isNight;

            // Inform day/night spawn manager.
            DayNightManager.getInstance().notifyChangeMode();

            GlobalEventListener.notify(new OnChangeDayCycle(isNight));

            // Set Shadow Sense skill to apply/remove effect from players.
            skill = SkillTable.getInstance().getInfo(L2Skill.SKILL_SHADOW_SENSE, 1);
        }

        // List is empty, skip.
        if (players.isEmpty()) {
            return;
        }

        // Loop all players.
        for (Map.Entry<Player, Integer> entry : players.entrySet()) {
            // Get player.
            Player player = entry.getKey();

            // Player isn't online, skip.
            if (!player.isOnline()) {
                continue;
            }

            // Shadow Sense skill is set and player has Shadow Sense skill, activate/deactivate its effect.
            if (skill != null && player.hasSkill(L2Skill.SKILL_SHADOW_SENSE)) {
                // Remove and add Shadow Sense to activate/deactivate effect.
                player.removeSkill(L2Skill.SKILL_SHADOW_SENSE, false);
                player.addSkill(skill, false);

                // Inform player about effect change.
                player.sendPacket(SystemMessage.getSystemMessage(isNight ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
            }

            // Activity time has passed already.
            if (time >= entry.getValue()) {
                // Inform player about his activity.
                player.sendPacket(SystemMessageId.PLAYING_FOR_LONG_TIME);

                // Update activity time.
                entry.setValue(time + TAKE_BREAK_GAME_MINUTES);
            }
        }
    }

    public void addQuestEvent(Quest quest) {
        quests.add(quest);
    }

    /**
     * Returns how many game days have left since last server start.
     *
     * @return int : Game day.
     */
    public int getGameDay() {
        return time / MINUTES_PER_DAY;
    }

    /**
     * Returns game time in minute format (0-1439).
     *
     * @return int : Game time.
     */
    public int getGameTime() {
        return time % MINUTES_PER_DAY;
    }

    /**
     * Returns game hour (0-23).
     *
     * @return int : Game hour.
     */
    public int getGameHour() {
        return (time % MINUTES_PER_DAY) / 60;
    }

    /**
     * Returns game minute (0-59).
     *
     * @return int : Game minute.
     */
    public int getGameMinute() {
        return time % 60;
    }

    /**
     * Returns game time standard format (00:00-23:59).
     *
     * @return String : Game time.
     */
    public String getGameTimeFormated() {
        return String.format("%02d:%02d", getGameHour(), getGameMinute());
    }

    /**
     * Returns game daytime. Night is between 00:00 and 06:00.
     *
     * @return boolean : True, when there is night.
     */
    public boolean isNight() {
        return getGameTime() < 360;
    }

    /**
     * Adds {@link Player} to the GameTimeTask to control is activity.
     *
     * @param player : {@link Player} to be added and checked.
     */
    public void add(Player player) {
        players.put(player, time + TAKE_BREAK_GAME_MINUTES);
    }

    /**
     * Removes {@link Player} from the GameTimeTask.
     *
     * @param player : {@link Player} to be removed.
     */
    public void remove(Player player) {
        players.remove(player);
    }

    public static GameTimeTaskManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static GameTimeTaskManager INSTANCE = new GameTimeTaskManager();
    }
}