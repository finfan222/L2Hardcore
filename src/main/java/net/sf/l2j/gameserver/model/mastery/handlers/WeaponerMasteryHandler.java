package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.events.OnHit;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author finfan
 */
public class WeaponerMasteryHandler implements MasteryHandler {

    private int calcSkillLevel(int maxSkillLevel, int playerLevel) {
        return (int) Math.max(Math.round(maxSkillLevel / 80. * playerLevel), 1);
    }

    @Override
    public void onLearn(Player player) {
        player.getEventListener().subscribe().group(this).cast(OnHit.class).forEach(this::onHit);
    }

    @Override
    public void onUnlearn(Player player) {
        MasteryHandler.super.onUnlearn(player);
    }

    private void onHit(OnHit event) {
        if (event.getHit().isMissed) {
            return;
        }

        Player player = (Player) event.getAttacker();
        Creature target = event.getTarget();
        if (target.isInvul()) {
            return;
        }

        WeaponType attackType = event.getAttacker().getAttackType();
        int level = player.getStatus().getLevel();
        switch (attackType) {
            case BIGBLUNT:
            case BLUNT:
                if (Rnd.calcChance(attackType == WeaponType.BIGBLUNT ? 10 : 5, 100)) {
                    if (level < 40) {
                        SkillTable.getInstance().getInfo(100, calcSkillLevel(15, level)).applyEffects(player, target);
                    } else {
                        SkillTable.getInstance().getInfo(260, calcSkillLevel(37, level)).applyEffects(player, target);
                    }
                }
                break;

            case SWORD:
            case BIGSWORD:
                if (Rnd.calcChance(attackType == WeaponType.BIGSWORD ? 14 : 7, 100)) {
                    if (level < 40) {
                        SkillTable.getInstance().getInfo(3, calcSkillLevel(9, level)).useSkill(player, new WorldObject[]{target});
                    } else {
                        SkillTable.getInstance().getInfo(255, calcSkillLevel(15, level)).useSkill(player, new WorldObject[]{target});
                    }
                }
                break;

            case DAGGER: // Mortal Blow
                double dexDiff = player.getStatus().getDEX() - player.getTemplate().getBaseDEX();
                if (Rnd.calcChance(dexDiff < 2 ? 1 : (int) Math.pow(1.1, dexDiff), 100)) {
                    SkillTable.getInstance().getInfo(16,calcSkillLevel(24, level)).useSkill(player, new WorldObject[]{target});
                }
                break;

            default:
                break;
        }
    }

}
