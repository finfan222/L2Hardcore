package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.events.OnAttacked;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Кастует на атакующего вас врага - Holy Strike. Может накастовать Holy Strike на себя, если вы являетесь PK или цель
 * не находится в режиме атаки (50/50)
 *
 * @author finfan
 */
public class HolyVengeanceMasteryHandler implements MasteryHandler {

    private void holyStrike(Player defender, Creature attacker, boolean playerIsEvil) {
        final L2Skill skill = defender.getSkill(49);
        if (skill == null) {
            return;
        }

        if (playerIsEvil && Rnd.nextBoolean()) {
            defender.sendMessage("Возмездие света настигает вас!");
            defender.broadcastPacket(new MagicSkillUse(defender, defender, 4011, 1, 1, 0));
            ThreadPool.schedule(() -> skill.useSkill(defender, new WorldObject[]{defender}), 666);
        } else {
            if (attacker instanceof Player) {
                attacker.sendMessage("Возмездие света настигает вас!");
            }
            defender.broadcastPacket(new MagicSkillUse(defender, attacker, 4011, 1, 1, 0));
            ThreadPool.schedule(() -> skill.useSkill(defender, new WorldObject[]{attacker}), 666);
        }
    }

    @Override
    public void onLearn(Player player) {
        player.getEventListener().subscribe().group(this).cast(OnAttacked.class).forEach(this::onAttacked);
    }

    @Override
    public void onUnlearn(Player player) {
        player.getEventListener().unsubscribe(this);
    }

    private void onAttacked(OnAttacked<?> event) {
        Creature attacker = event.getAttacker();
        Player defender = (Player) event.getTarget();

        int level = defender.getStatus().getLevel();
        if (!Rnd.calcChance(level, 100)) {
            return;
        }

        boolean attackerIsEvil = attacker.isAttackableWithoutForceBy(defender);
        if (attackerIsEvil) {
            holyStrike(defender, attacker, false);
        } else {
            holyStrike(defender, attacker, defender.getKarma() > 0 || (attacker instanceof Player pc && pc.getPvpFlag() == 0 && defender.getPvpFlag() > 0));
        }
    }

}
