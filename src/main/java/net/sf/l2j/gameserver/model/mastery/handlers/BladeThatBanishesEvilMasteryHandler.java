package net.sf.l2j.gameserver.model.mastery.handlers;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.Mastery;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * 1 lvl atkVsUndead = x1000, atkVsDemons = x1000, pAtk = x1000<br>
 * 2 lvl atkVsUndead = x1000, atkVsDemons = x1000<br>
 * 3 lvl atkVsUndead = x100, atkVsDemons = x100<br>
 * 4 lvl atkVsUndead = x100
 * @author finfan
 */
public class BladeThatBanishesEvilMasteryHandler implements MasteryHandler {

    private static final int MAX_CHANCE = 100000000;
    private static final int SKILL_ID = 1;
    private static final int SKILL_MAX_LEVEL = 4;

    @Override
    public void onLearn(Player player) {
        player.getMastery().addTask(new Mastery.MasteryTask(this, ThreadPool.scheduleAtFixedRate(() -> {
            if (player.getFirstEffect(SKILL_ID) != null) {
                return;
            }

            L2Skill buff = SkillTable.getInstance().getInfo(SKILL_ID, calcChargeLevel(player));
            player.sendPacket(new PlaySound("skillsound.disrupt_undead_cast")); // звук energy stone
            player.sendPacket(new MagicSkillUse(player, 1, 1, 0, 0)); // эффект бафа
            buff.applyEffects(player, player);
            switch (buff.getLevel()) {
                case 1:
                    player.sendMessage("Оружие заряжено силой света Эйнхасад! Теперь ваша мощь способна уничтожить все с одного удара.");
                    break;
                case 2:
                    player.sendMessage("Оружие заряжено силой света Эйнхасад! Теперь ваша мощь способна уничтожать очень сильных нежить и демонов с одного удара.");
                    break;
                case 3:
                    player.sendMessage("Оружие заряжено силой света Эйнхасад! Теперь ваша мощь способна уничтожать нежить и демонов равной вам силы с одного удара.");
                    break;
                case 4:
                    player.sendMessage("Оружие заряжено силой света Эйнхасад! Теперь ваша мощь способна уничтожить любую нежить равной вам силы с одного удара.");
                    break;
                default:
                    throw new UnsupportedOperationException("Skill ID=" + SKILL_ID + " dont have a level=" + buff.getLevel());
            }

        }, 0, 60_000)));
    }

    private int calcChargeLevel(Player player) {
        int chance = 10;
        int level = 4;
        for (int i = 0; i < SKILL_MAX_LEVEL; i++) {
            if (Rnd.calcChance(chance, MAX_CHANCE)) {
                level = i + 1;
                break;
            }

            chance = (int) Math.pow(chance, 2);
        }

        return level;
    }

    @Override
    public void onUnlearn(Player player) {
        player.getMastery().removeTask(this);
    }

}
