package net.sf.l2j.gameserver.skills.handlers;

import net.sf.l2j.Config;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Cancel extends Default {

    public Cancel(StatSet set) {
        super(set);
    }

    @Override
    public boolean isSkillTypeOffensive() {
        return true;
    }

    private static boolean calcCancelSuccess(int effectPeriod, int diffLevel, double baseRate, double vuln, int minRate, int maxRate) {
        double rate = (2 * diffLevel + baseRate + effectPeriod / 120.) * vuln;

        if (Config.DEVELOPER) {
            LOGGER.info("calcCancelSuccess(): diffLevel:{}, baseRate:{}, vuln:{}, total:{}.", diffLevel, baseRate, vuln, rate);
        }

        return Rnd.get(100) < MathUtil.limit((int) rate, minRate, maxRate);
    }

    @Override
    public void useSkill(Creature caster, WorldObject[] targets) {
        // Delimit min/max % success.
        final int minRate = (getSkillType() == SkillType.CANCEL) ? 25 : 40;
        final int maxRate = (getSkillType() == SkillType.CANCEL) ? 75 : 95;

        // Get skill power (which is used as baseRate).
        final double skillPower = getPower();

        for (WorldObject obj : targets) {
            if (!(obj instanceof Creature target)) {
                continue;
            }

            if (target.isDead()) {
                continue;
            }

            Context context = Context.builder().build();
            int count = getMaxNegatedEffects();

            // Calculate the difference of level between skill level and victim, and retrieve the vuln/prof.
            final int diffLevel = getMagicLevel() - target.getStatus().getLevel();
            final double resistance = target.getStatus().calcStat(Stats.CANCEL_VULN, 1.0, target, null);

            final List<AbstractEffect> list = Arrays.asList(target.getAllEffects());
            Collections.shuffle(list);

            for (AbstractEffect effect : list) {
                // Don't cancel toggles or debuffs.
                if (effect.getSkill().isToggle() || effect.getSkill().isDebuff()) {
                    continue;
                }

                // Don't cancel specific EffectTypes.
                if (EffectType.isntCancellable(effect.getEffectType())) {
                    continue;
                }

                // Mage && Warrior Bane drop only particular stacktypes.
                switch (getSkillType()) {
                    case MAGE_BANE:
                        if ("casting_time_down".equalsIgnoreCase(effect.getTemplate().getStackType())) {
                            break;
                        }

                        if ("ma_up".equalsIgnoreCase(effect.getTemplate().getStackType())) {
                            break;
                        }

                        continue;

                    case WARRIOR_BANE:
                        if ("attack_time_down".equalsIgnoreCase(effect.getTemplate().getStackType())) {
                            break;
                        }

                        if ("speed_up".equalsIgnoreCase(effect.getTemplate().getStackType())) {
                            break;
                        }

                        continue;
                }

                // Calculate the success chance following previous variables.
                context.isSuccess = calcCancelSuccess(effect.getPeriod(), diffLevel, skillPower, resistance, minRate, maxRate);
                if (context.isSuccess) {
                    effect.exit();
                }

                // Remove 1 to the stack of buffs to remove.
                count--;

                // If the stack goes to 0, then break the loop.
                if (count == 0) {
                    break;
                }
            }

            context.value = context.isSuccess ? (int) Formulas.calcNegateSkillPower(this, caster, target) : 0;
            notifyAboutSkillHit(caster, target, context);
        }

        if (hasSelfEffects()) {
            final AbstractEffect effect = caster.getFirstEffect(getId());
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }

            applySelfEffects(caster);
        }
        caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());

    }
}