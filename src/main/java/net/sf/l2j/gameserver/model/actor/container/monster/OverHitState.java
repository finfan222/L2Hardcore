package net.sf.l2j.gameserver.model.actor.container.monster;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
@RequiredArgsConstructor
public class OverHitState {

    private final AtomicBoolean overHit = new AtomicBoolean(false);
    private final Monster monster;

    private Creature overHitOwner;
    private double overHitDamage;

    public boolean activate() {
        return overHit.compareAndSet(false, true);
    }

    public void check(Creature attacker, double damage) {
        // Over-hit wasn't procced, simply return.
        if (!overHit.get()) {
            return;
        }

        // No damage.
        if (damage <= 0) {
            overHit.set(false);
            return;
        }

        // Calculate the over-hit damage.
        final double overHitDamage = ((monster.getStatus().getHp() - damage) * (-1));

        // Not enough damage to kill the Monster.
        if (overHitDamage < 0) {
            overHit.set(false);
            return;
        }

        // Over-hit is a success, set variables.
        this.overHitDamage = overHitDamage;
        this.overHitOwner = attacker;
    }

    public void clear() {
        overHit.set(false);
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
        double percentage = ((overHitDamage * 100) / monster.getStatus().getMaxHp());
        if (percentage > 100) {
            percentage = 100;
        }

        // Get the over-hit exp bonus according to the above over-hit damage percentage.
        double overExp = ((percentage / 100) * normalExp * 5.0) * Math.pow(0.8333, ownerLevel - monsterLevel - 5);

        // Return the rounded amount of exp points to be added to the player's normal exp reward.
        return Math.round(overExp);
    }

    public boolean isValidOverHit(Player player) {
        return overHit.get() && overHitOwner != null && overHitOwner.getActingPlayer() != null && player == overHitOwner.getActingPlayer();
    }
}