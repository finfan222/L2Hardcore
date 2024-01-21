package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

import java.util.Map;

public class Resurrect extends Default {

    public Resurrect(StatSet set) {
        super(set);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        for (WorldObject cha : targets) {
            final Creature target = (Creature) cha;
            if (caster instanceof Player player) {
                if (cha instanceof Player playerTarget) {
                    playerTarget.reviveRequest(player, this, false);
                } else if (cha instanceof Pet pet) {
                    if (pet.getOwner() == caster) {
                        target.doRevive(Formulas.calculateSkillResurrectRestorePercent(getPower(), caster));
                    } else {
                        pet.getOwner().reviveRequest((Player) caster, this, true);
                    }
                } else {
                    target.doRevive(Formulas.calculateSkillResurrectRestorePercent(getPower(), caster));
                }
            } else {
                DecayTaskManager.getInstance().cancel(target);
                target.doRevive(Formulas.calculateSkillResurrectRestorePercent(getPower(), caster));
            }
            notifyAboutSkillHit(caster, target, Map.of("damage", Formulas.calcNegateSkillPower(this, caster, target)));
        }
        caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());

    }
}