package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

public class Resurrect extends Default {

    public Resurrect(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        for (WorldObject cha : targets) {
            final Creature target = (Creature) cha;
            Context context = Context.builder().build();
            if (caster instanceof Player player) {
                if (target instanceof Player playerTarget) {
                    playerTarget.reviveRequest(player, this, false);
                } else if (target instanceof Pet pet) {
                    if (pet.getOwner() == caster) {
                        context.value = Formulas.calculateSkillResurrectRestorePercent(getPower(), caster);
                        target.doRevive(context.value);
                    } else {
                        pet.getOwner().reviveRequest((Player) caster, this, true);
                    }
                } else {
                    context.value = Formulas.calculateSkillResurrectRestorePercent(getPower(), caster);
                    target.doRevive(context.value);
                }
            } else {
                DecayTaskManager.getInstance().cancel(target);
                context.value = Formulas.calculateSkillResurrectRestorePercent(getPower(), caster);
                target.doRevive(context.value);
            }

            notifyAboutSkillHit(caster, target, context);
        }
        caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());

    }

}