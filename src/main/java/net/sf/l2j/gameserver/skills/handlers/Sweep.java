package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.List;

public class Sweep extends L2Skill {

    public Sweep(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        if (!(caster instanceof Player player)) {
            return;
        }

        for (WorldObject target : targets) {
            if (!(target instanceof Monster monster)) {
                continue;
            }

            final List<IntIntHolder> items = monster.getSpoilState();
            if (items.isEmpty()) {
                continue;
            }

            // Reward spoiler, based on sweep items retained on List.
            for (IntIntHolder item : items) {
                if (player.isInParty()) {
                    player.getParty().distributeItem(player, item, true, monster);
                } else {
                    player.addItem("Sweep", item.getId(), item.getValue(), player, true);
                }
            }

            // Reset variables.
            monster.getSpoilState().clear();
        }

        if (hasSelfEffects()) {
            applySelfEffects(caster);
        }
    }
}