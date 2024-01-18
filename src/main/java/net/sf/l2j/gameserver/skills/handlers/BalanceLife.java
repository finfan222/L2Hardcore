package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.ArrayList;
import java.util.List;

public class BalanceLife extends L2Skill {

    public BalanceLife(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        final Player player = caster.getActingPlayer();
        final List<Creature> finalList = new ArrayList<>();

        double fullHP = 0;
        double currentHPs = 0;

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isDead()) {
                continue;
            }

            // Player holding a cursed weapon can't be healed and can't heal
            if (target != caster) {
                if (target instanceof Player && ((Player) target).isCursedWeaponEquipped()) {
                    continue;
                } else if (player != null && player.isCursedWeaponEquipped()) {
                    continue;
                }
            }

            fullHP += target.getStatus().getMaxHp();
            currentHPs += target.getStatus().getHp();

            // Add the character to the final list.
            finalList.add(target);
        }

        if (!finalList.isEmpty()) {
            double percentHP = currentHPs / fullHP;

            for (Creature target : finalList) {
                target.getStatus().setHp(target.getStatus().getMaxHp() * percentHP);

                if (hasEffects()) {
                    target.stopSkillEffects(getId());
                    applyEffects(caster, target);
                }
            }
        }
    }
}