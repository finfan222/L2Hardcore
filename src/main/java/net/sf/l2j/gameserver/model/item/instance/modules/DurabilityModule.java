package net.sf.l2j.gameserver.model.item.instance.modules;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemDao;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.mastery.MasteryUtil;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.handlers.Default;

import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@Slf4j
@Data
public class DurabilityModule implements ItemModule {

    private final ItemInstance item;

    private int durability;
    private long timestamp;

    public DurabilityModule(ItemInstance item) {
        this.item = item;
        this.durability = Short.MAX_VALUE;
        this.timestamp = System.currentTimeMillis();
    }

    private void fracture(Player player, int value) {
        durability -= value;
        if (isTimeToUpdate()) {
            update(player);
        }

        tryBreakItem(player);
    }

    public void repair(Player player) {
        durability = Short.MAX_VALUE;
        update(player);
    }

    public void tryBreakItem(Player player) {
        if (durability == -1 || durability > 0) {
            return; // unbreakable
        }

        synchronized (item) {
            player.sendPacket(new PlaySound("ItemSound.trash_basket"));
            player.sendMessage(String.format("[FIX: SYSMSG] Предмет %s был сломан!", item.getName()));
            player.destroyItem("DurabilityBrokeItem", item, 1, player, true);
            player.sendPacket(new ItemList(player, false));
        }
    }

    public int getReferencePrice() {
        return (int) (item.getItem().getReferencePrice() * breakMod());
    }

    private double breakMod() {
        return durability * 1D / Short.MAX_VALUE;
    }

    private boolean isTimeToUpdate() {
        return System.currentTimeMillis() > timestamp;
    }

    private void update(Player player) {
        timestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        ItemDao.updateDurability(item);
        final InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(item);
        player.sendPacket(iu);
    }

    public int getDurabilityPercent() {
        return (int) Math.max(breakMod() * 100.0, 1);
    }

    public int getRepairPrice() {
        CrystalType grade = item.getItem().getCrystalType();
        double gradeModifier = (grade.getId() * 1. / grade.getRepairModifier()) + 1;
        return (int) Math.pow(getReferencePrice(), gradeModifier);
    }

    public void fractureWeapon(Player player, Creature target, L2Skill skill, Default.Context context) {
        // on missed we reduce durability if attack was from BOW
        if (context.isMissed() && player.getAttackType() != WeaponType.BOW) {
            return;
        }

        final int value = Formulas.calcWeaponFractureValue(player, target, skill, context, item);
        if (value > 0) {
            if (player.isGM()) {
                player.sendMessage("Потеряно прочности у " + item.getItemName() + "=" + value);
            }
            fracture(player, value);
        }
    }

    public void fractureArmor(Player player, L2Skill skill, Default.Context context) {
        if (context.isMissed()) {
            return;
        }

        final int value = MasteryUtil.VeteranMastery_invoke(player, item) ? 1 : Formulas.calcArmorFractureValue(skill, context, item.getArmorItem());
        if (value > 0) {
            if (player.isGM()) {
                player.sendMessage("Потеряно прочности у " + item.getItemName() + "=" + value);
            }
            fracture(player, value);
        }
    }
}
