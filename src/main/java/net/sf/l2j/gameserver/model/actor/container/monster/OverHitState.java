package net.sf.l2j.gameserver.model.actor.container.monster;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class OverHitState {

    private final ReentrantLock locker = new ReentrantLock();
    private final Monster monster;

    private Creature overHitOwner;
    private double overHitDamage;
    private boolean isActivated;

    public void check(Creature attacker, double damage) {
        // Over-hit wasn't proceed, simply return.
        if (!isActivated) {
            return;
        }

        // No damage.
        if (damage <= 0) {
            isActivated = false;
            return;
        }

        // Calculate the over-hit damage.
        final double overHitDamage = ((monster.getStatus().getHp() - damage) * (-1));

        // Not enough damage to kill the Monster.
        if (overHitDamage < 0) {
            isActivated = false;
            return;
        }

        // Over-hit is a success, set variables.
        this.overHitDamage = overHitDamage;
        this.overHitOwner = attacker;
    }

    public void clear() {
        isActivated = false;
        overHitDamage = 0;
        overHitOwner = null;
    }

    public long calcExp(long normalExp) {
        if (overHitOwner == null) {
            return 0;
        }

        int ownerLevel = overHitOwner.getStatus().getLevel();
        int monsterLevel = monster.getStatus().getLevel();

        // Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) amount of HP.
        double overExp = getOverExp(normalExp, ownerLevel, monsterLevel);

        // Return the rounded amount of exp points to be added to the player's normal exp reward.
        return Math.round(overExp);
    }

    private double getOverExp(long normalExp, int ownerLevel, int monsterLevel) {
        double percentage = ((overHitDamage * 100) / monster.getStatus().getMaxHp());
        if (percentage > 100) {
            percentage = 100;
        }

        // Get the over-hit exp bonus according to the above over-hit damage percentage.
        return ((percentage / 100) * normalExp * Config.HARDCORE_RATE_OVERHIT_EXP) * Math.pow(0.8333, ownerLevel - monsterLevel - 5);
    }

    public boolean isValidOverHit(Player player) {
        return isActivated && overHitOwner != null && overHitOwner.getActingPlayer() != null && player == overHitOwner.getActingPlayer();
    }
}