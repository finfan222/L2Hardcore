package net.sf.l2j.gameserver.taskmanager;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import it.sauronsoftware.cron4j.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.events.OnDayCycleChange;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@Slf4j
public class DayNightTaskManager implements Runnable {

    @Getter(lazy = true)
    private static final DayNightTaskManager instance = new DayNightTaskManager();

    private static final String CRON_PATTERN = "* * * * *";

    @Getter private DayCycle currentCycle;
    private DayCycle previousCycle;
    private int cycleDiff;

    private DayNightTaskManager() {
        Scheduler scheduler = new Scheduler();
        scheduler.schedule(CRON_PATTERN, this);
        scheduler.start();
        currentCycle = calcCurrentCycle();
        previousCycle = calcPreviousCycle();
        cycleDiff = currentCycle.ordinal() - previousCycle.ordinal();
        log.info("Loaded successfully!");
        log.debug("[{} -> {}] Next execution date-time: {}", previousCycle, currentCycle, getNextExecutionDateTime());
    }

    /**
     * @return {@link ZonedDateTime} of next {@code cron} schedule
     * @see DayNightTaskManager#CRON_PATTERN
     */
    private ZonedDateTime getNextExecutionDateTime() {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J);
        CronParser cronParser = new CronParser(cronDefinition);
        ExecutionTime executionTime = ExecutionTime.forCron(cronParser.parse(CRON_PATTERN));
        Optional<ZonedDateTime> zonedDateTime = executionTime.nextExecution(getTime().atZone(ZoneId.systemDefault()));
        if (zonedDateTime.isPresent()) {
            return zonedDateTime.get();
        }

        throw new RuntimeException("NO NEXT EXECUTION DATE FOR DAY CHANGE CYCLE MANAGER");
    }

    /**
     * @return {@link Calendar#getTimeInMillis()} of 1970.01.01 to this day at 00:00:00 in milliseconds
     */
    private long zeroOfADay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * @return {@link LocalDateTime#now()}
     */
    public LocalDateTime getTime() {
        return LocalDateTime.now();
    }

    /**
     * @return current time in millis (from 1970.01.01) equal to {@link System#currentTimeMillis()}
     */
    public long getTimeInMillis() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @return time of a day in milliseconds (max_millis: 86.400.000)
     */
    private long getDayTimeInMillis() {
        return getTimeInMillis() - zeroOfADay();
    }

    /**
     * Also knows as GameTime.
     *
     * @return time of a day in minutes (max_minutes: 1440)
     */
    public int getDayTime() {
        return (int) TimeUnit.MILLISECONDS.toMinutes(getDayTimeInMillis());
    }

    /**
     * @param cycle {@link DayCycle}
     * @return check if {@link DayNightTaskManager#getTime()} is after {@link DayCycle#getStart()} and is before
     * {@link DayCycle#getEnd()}
     */
    public boolean is(DayCycle cycle) {
        int dayTime = getDayTime();
        return dayTime >= cycle.getStart() && dayTime < cycle.getEnd();
    }

    /**
     * @return return {@link DayCycle} if {@link DayNightTaskManager#is(DayCycle)} in
     * @throws RuntimeException if in {@link DayCycle#values()} not found any value matches to this check
     */
    private DayCycle calcCurrentCycle() {
        for (DayCycle next : DayCycle.values()) {
            if (is(next)) {
                return next;
            }
        }

        throw new RuntimeException("Wrong calculating of game time cycle.");
    }

    /**
     * Calculates previous day cycle which dependable from {@link DayNightTaskManager#currentCycle}.
     * <font color="RED">Must be calculated</font> {@code after} {@link DayNightTaskManager#currentCycle}.
     *
     * @return {@link DayNightTaskManager#previousCycle}
     */
    private DayCycle calcPreviousCycle() {
        if (currentCycle == DayCycle.MORNING) {
            previousCycle = DayCycle.NIGHT;
        } else {
            previousCycle = DayCycle.values()[currentCycle.ordinal() - 1];
        }
        return previousCycle;
    }

    @Override
    public void run() {
        currentCycle = calcCurrentCycle();
        previousCycle = calcPreviousCycle();
        int difference = currentCycle.getId() - previousCycle.getId();
        if (cycleDiff != difference) {
            cycleDiff = difference;
            GlobalEventListener.notify(new OnDayCycleChange(currentCycle, previousCycle));
        }
        log.debug("[{} -> {}] Next execution date-time: {}", previousCycle, currentCycle, getNextExecutionDateTime().toLocalTime());
        log.debug("Current game day time: {}", getDayTime());
    }

    public String getGameTimeFormatted() {
        LocalTime gameTime = getTime().toLocalTime();
        return String.format("%02d:%02d:%02d", gameTime.getHour(), gameTime.getMinute(), gameTime.getSecond());
    }
}
