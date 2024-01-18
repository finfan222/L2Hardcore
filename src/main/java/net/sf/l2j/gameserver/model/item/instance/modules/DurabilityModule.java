package net.sf.l2j.gameserver.model.item.instance.modules;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemDao;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;

import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
@Slf4j
@Data
public class DurabilityModule implements ItemModule {

    private final ItemInstance instance;

    private int durability;
    private long timestamp;

    public DurabilityModule(ItemInstance instance) {
        this.instance = instance;
        this.durability = Short.MAX_VALUE;
        this.timestamp = System.currentTimeMillis();
    }

    public void fracture(Player player, int value) {
        durability -= (int) Math.sqrt(value);
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

        synchronized (instance) {
            player.sendPacket(new PlaySound("ItemSound.trash_basket"));
            player.sendMessage(String.format("Item %s was broken", instance.getName()));
            player.destroyItem("DurabilityBrokeItem", instance, 1, player, true);
        }
    }

    public int getReferencePrice() {
        return (int) (instance.getItem().getReferencePrice() * breakMod());
    }

    private double breakMod() {
        return durability * 1D / Short.MAX_VALUE;
    }

    private boolean isTimeToUpdate() {
        return System.currentTimeMillis() > timestamp;
    }

    private void update(Player player) {
        timestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        ItemDao.updateDurability(instance);
        final InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(instance);
        player.sendPacket(iu);
    }

    public int getDurabilityPercent() {
        return (int) (breakMod() * 100.0);
    }

    public int getRepairPrice() {
        CrystalType grade = instance.getItem().getCrystalType();
        int currentDurability = Short.MAX_VALUE - durability;
        double gradeModifier = (grade.getId() * 1. / grade.getRepairModifier()) + 1;
        return (int) Math.pow(currentDurability, gradeModifier);
    }

}
