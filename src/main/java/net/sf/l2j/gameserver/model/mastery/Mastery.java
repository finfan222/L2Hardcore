package net.sf.l2j.gameserver.model.mastery;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.events.OnLevelChange;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.repository.MasteryRepository;
import net.sf.l2j.gameserver.network.SystemMessageColor;

import java.util.Arrays;

/**
 * @author finfan
 */
@Getter
@Slf4j
public class Mastery {

    private final Player player;
    private final MasteryData[] values;
    private final MasteryRepository repository;

    private int nextIndex;

    @Setter
    private int points;

    public Mastery(Player player) {
        this.player = player;
        this.values = new MasteryData[MasteryManager.MAX_MASTERY_LEARN];
        this.repository = new MasteryRepository();
        player.getEventListener().subscribe().cast(OnLevelChange.class).forEach(this::onLevelChange);
    }

    private synchronized void onLevelChange(OnLevelChange event) {
        int diff = event.getNewLevel() - event.getOldLevel();
        if (diff <= 0) {
            return;
        }

        diff = diff / 10;

        log.info("Possible points: {}", diff);
        points += diff;
        player.sendMessage("Вы получили " + diff + " очков мастерства!", SystemMessageColor.GREEN_LIGHT);
    }

    public boolean isHasMastery(int id) {
        for (MasteryData next : values) {
            if (next != null && next.getId() == id) {
                return true;
            }
        }
        return false;
    }

    public MasteryData getMasteryData(int id) {
        for (MasteryData next : values) {
            if (next != null && next.getId() == id) {
                return next;
            }
        }

        return null;
    }

    public MasteryData getMasteryData(String name) {
        for (MasteryData masteryData : values) {
            if (masteryData != null && masteryData.getName().equals(name)) {
                return masteryData;
            }
        }

        return null;
    }

    public synchronized void addMastery(MasteryData masteryData) {
        values[nextIndex] = masteryData;
        nextIndex++;
        if (masteryData.getHandler() != null) {
            masteryData.getHandler().onLearn(player, masteryData);
        }
    }

    public synchronized void resetMastery() {
        for (MasteryData next : values) {
            if (next.getHandler() != null) {
                next.getHandler().onUnlearn(player, next);
            }
        }
        Arrays.fill(values, null);
        points = (player.getStatus().getLevel() - 10) / 10;
        nextIndex = 0;
        repository.delete(player.getObjectId(), points);
    }

    public synchronized void learnMastery(MasteryData masteryData) {
        addMastery(masteryData);
        points--;
        repository.create(this, masteryData.getId());
    }

    public void restore() {
        repository.restore(this);
    }

}
