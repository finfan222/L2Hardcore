package net.sf.l2j.gameserver.model.item.instance;

import lombok.Getter;
import lombok.Setter;
import net.sf.l2j.Config;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.enums.items.ItemType;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.Augmentation;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.MercenaryTicket;
import net.sf.l2j.gameserver.model.item.instance.modules.DurabilityModule;
import net.sf.l2j.gameserver.model.item.instance.modules.ItemModule;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.DropItem;
import net.sf.l2j.gameserver.network.serverpackets.GetItem;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItem;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This class manages items.
 */
public final class ItemInstance extends WorldObject implements Comparable<ItemInstance> {

    @Getter
    private final ItemInstanceData data;

    @Getter
    @Setter
    private Augmentation augmentation;
    private int shotsMask;

    private Map<Class<? extends ItemModule>, ItemModule> modules = new HashMap<>();

    public ItemInstance(int objectId, int itemId) {
        super(objectId);
        data = ItemInstanceData.builder()
            .itemId(itemId)
            .count(1)
            .location(ItemLocation.VOID)
            .lastChange(ItemState.MODIFIED)
            .build();
    }

    public boolean isHerb() {
        return getItem().getItemType() == EtcItemType.HERB;
    }

    public ItemInstance(int objectId, Item item) {
        this(objectId, item.getItemId());
    }

    public <T extends ItemModule> void registerModule(T module) {
        modules.put(module.getClass(), module);
        module.onRegister(this);
    }

    @SuppressWarnings("unchecked")
    public <T extends ItemModule> T getModule(Class<T> type) {
        return (T) modules.get(type);
    }

    public void setOwnerId(String process, int ownerId, Player creator, WorldObject reference) {
        data.setOwnerId(ownerId);

        if (Config.LOG_ITEMS) {
            final LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]
                {
                    creator,
                    this,
                    reference
                });
            ItemFactory.ITEM_LOG.log(record);
        }
    }

    public void setCount(int count) {
        if (getCount() == count) {
            return;
        }

        data.setCount(count >= -1 ? count : 0);
    }

    public int getOwnerId() {
        return data.getOwnerId();
    }

    public void setEnchantLevel(int level) {
        data.setEnchantLevel(level);
    }

    public int getEnchantLevel() {
        return data.getEnchantLevel();
    }

    public ItemLocation getLocation() {
        return data.getLocation();
    }

    public int getCount() {
        return data.getCount();
    }

    public void changeCount(String process, int count, Player creator, WorldObject reference) {
        if (count == 0) {
            return;
        }

        if (count > 0 && getCount() > Integer.MAX_VALUE - count) {
            setCount(Integer.MAX_VALUE);
        } else {
            setCount(getCount() + count);
        }

        if (getCount() < 0) {
            setCount(0);
        }

        if (Config.LOG_ITEMS && process != null) {
            final LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]{
                creator,
                this,
                reference
            });
            ItemFactory.ITEM_LOG.log(record);
        }
    }

    public boolean isEquipable() {
        Item item = data.get();
        return !(item.getBodyPart() == 0 || item.getItemType() == EtcItemType.ARROW || item.getItemType() == EtcItemType.LURE);
    }

    public boolean isEquipped() {
        return data.getLocation() == ItemLocation.PAPERDOLL || data.getLocation() == ItemLocation.PET_EQUIP;
    }

    public int getSlot() {
        return data.getSlot();
    }

    public Item getItem() {
        return data.get();
    }

    public int getCustomType1() {
        return data.getCustomType1();
    }

    public int getCustomType2() {
        return data.getCustomType2();
    }

    public void setCustomType1(int type) {
        data.setCustomType1(type);
    }

    public void setCustomType2(int type) {
        data.setCustomType2(type);
    }

    public boolean isOlyRestrictedItem() {
        return getItem().isOlyRestrictedItem();
    }

    public ItemType getItemType() {
        return data.get().getItemType();
    }

    public int getItemId() {
        return data.getItemId();
    }

    public boolean isEtcItem() {
        return (data.get() instanceof EtcItem);
    }

    public boolean isWeapon() {
        return (data.get() instanceof Weapon);
    }

    public boolean isArmor() {
        return (data.get() instanceof Armor);
    }

    public EtcItem getEtcItem() {
        return data.get() instanceof EtcItem item ? item : null;
    }

    public Weapon getWeaponItem() {
        return data.get() instanceof Weapon item ? item : null;
    }

    public Armor getArmorItem() {
        return data.get() instanceof Armor item ? item : null;
    }

    public int getCrystalCount() {
        return data.getCrystalCount();
    }

    public int getReferencePrice() {
        return data.get().getReferencePrice();
    }

    public String getItemName() {
        return data.get().getName();
    }

    public ItemState getLastChange() {
        return data.getLastChange();
    }

    public void setLastChange(ItemState lastChange) {
        data.setLastChange(lastChange);
    }

    public boolean isStackable() {
        return data.get().isStackable();
    }

    public boolean isDropable() {
        return data.get().isDropable();
    }

    public boolean isDestroyable() {
        return !isQuestItem() && data.get().isDestroyable();
    }

    public boolean isTradable() {
        return data.get().isTradable();
    }

    public boolean isSellable() {
        return data.get().isSellable();
    }

    public boolean isDepositable(boolean isPrivateWareHouse) {
        if (isEquipped() || !data.get().isDepositable()) {
            return false;
        }

        if (!isPrivateWareHouse) {
            return isTradable();
        }

        return true;
    }

    public boolean isConsumable() {
        return data.get().isConsumable();
    }

    /**
     * @param player : the player to check.
     * @param allowAdena : if true, count adenas.
     * @param allowNonTradable : if true, count non tradable items.
     * @param allowStoreBuy
     * @return if item is available for manipulation.
     */
    public boolean isAvailable(Player player, boolean allowAdena, boolean allowNonTradable, boolean allowStoreBuy) {
        return ((!isEquipped() || allowStoreBuy) // Not equipped
            && (getItem().getType2() != Item.TYPE2_QUEST) // Not Quest Item
            && (getItem().getType2() != Item.TYPE2_MONEY || getItem().getType1() != Item.TYPE1_SHIELD_ARMOR) // not money, not shield
            && (player.getSummon() == null || getObjectId() != player.getSummon().getControlItemObjectId()) // Not Control item of currently summoned pet
            && (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
            && (allowAdena || getItemId() != 57) // Not adena
            && (player.getCast().getCurrentSkill() == null || player.getCast().getCurrentSkill().getItemConsumeId() != getItemId()) && (allowNonTradable || isTradable()));
    }

    @Override
    public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed) {
        if (player.isFlying()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Mercenaries tickets case.
        if (getItem().getItemType() == EtcItemType.CASTLE_GUARD) {
            if (player.isInParty()) {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            final Castle castle = CastleManager.getInstance().getCastle(player);
            if (castle == null) {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            final MercenaryTicket ticket = castle.getTicket(data.getItemId());
            if (ticket == null) {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            if (!player.isCastleLord(castle.getCastleId())) {
                player.sendPacket(SystemMessageId.THIS_IS_NOT_A_MERCENARY_OF_A_CASTLE_THAT_YOU_OWN_AND_SO_CANNOT_CANCEL_POSITIONING);
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
        }

        player.getAI().tryToPickUp(getObjectId(), isShiftPressed);
    }

    public boolean isAugmented() {
        return augmentation != null;
    }

    public List<Func> getStats(Creature player) {
        return getItem().getStatFuncs(this, player);
    }

    /**
     * Validate intended dropping location, set it and spawn this {@link ItemInstance} to the world.
     *
     * @param dropper : The {@link Creature} dropper.
     * @param x : The X coordinate of intended location.
     * @param y : The Y coordinate of intended location.
     * @param z : The Z coordinate of intended location.
     */
    public final void dropMe(Creature dropper, int x, int y, int z) {
        ThreadPool.execute(() ->
        {
            // Set the dropper OID for sendInfo show correct dropping animation.
            setDropperObjectId(dropper.getObjectId());

            // Drop current World registration, mostly for FREIGHT case.
            World.getInstance().removeObject(this);

            // Validate location and spawn.
            spawnMe(GeoEngine.getInstance().getValidLocation(dropper, x, y, z));
            ItemsOnGroundTaskManager.getInstance().add(this, dropper);

            // Set the dropper OID back to 0, so sendInfo show item on ground.
            setDropperObjectId(0);
        });
    }

    /**
     * Calculate dropping location from {@link Creature} location and offset, validate it, set it and spawn this
     * {@link ItemInstance} to the world.
     *
     * @param dropper : The {@link Creature} dropper.
     * @param offset : The offset used to calculate dropping location around {@link Creature}.
     */
    public final void dropMe(Creature dropper, int offset) {
        // Create drop location.
        final Location loc = dropper.getPosition().clone();
        loc.addRandomOffset(offset);

        ThreadPool.execute(() ->
        {
            // Set the dropper OID for sendInfo show correct dropping animation.
            setDropperObjectId(dropper.getObjectId());

            // Drop current World registration, mostly for FREIGHT case.
            World.getInstance().removeObject(this);

            // Validate location itself and spawn.
            spawnMe(GeoEngine.getInstance().getValidLocation(dropper, loc));
            ItemsOnGroundTaskManager.getInstance().add(this, dropper);

            // Set the dropper OID back to 0, so sendInfo show item on ground.
            setDropperObjectId(0);
        });
    }

    /**
     * Remove a ItemInstance from the visible world and send server->client GetItem packets.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _objects of
     * World.</B></FONT><BR>
     * <BR>
     *
     * @param player Player that pick up the item
     */
    public void pickupMe(Creature player) {
        player.broadcastPacket(new GetItem(this, player.getObjectId()));

        // Unregister dropped ticket from castle, if that item is on a castle area and is a valid ticket.
        final Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle != null && castle.getTicket(data.getItemId()) != null) {
            castle.removeDroppedTicket(this);
        }

        if (data.getItemId() == 57 || data.getItemId() == 6353) {
            final Player actor = player.getActingPlayer();
            if (actor != null) {
                final QuestState qs = actor.getQuestList().getQuestState("Tutorial");
                if (qs != null) {
                    qs.getQuest().notifyEvent("CE" + data.getItemId() + "", null, actor);
                }
            }
        }

        // Calls directly setRegion(null), we don't have to care about.
        setIsVisible(false);
    }

    /**
     * @return the item in String format.
     */
    @Override
    public String toString() {
        return "(" + getObjectId() + ") " + getName();
    }

    public boolean isNightLure() {
        return ((data.getItemId() >= 8505 && data.getItemId() <= 8513) || data.getItemId() == 8485);
    }

    public boolean isPetItem() {
        return getItem().isPetItem();
    }

    public boolean isPotion() {
        return getItem().isPotion();
    }

    public boolean isElixir() {
        return getItem().isElixir();
    }

    public boolean isSummonItem() {
        return getItem().getItemType() == EtcItemType.PET_COLLAR;
    }

    public boolean isHeroItem() {
        return getItem().isHeroItem();
    }

    public boolean isQuestItem() {
        return getItem().isQuestItem();
    }

    @Override
    public void decayMe() {
        ItemsOnGroundTaskManager.getInstance().remove(this);
        super.decayMe();
    }

    public void setLocation(ItemLocation location) {
        data.setLocation(location);
    }

    public void setSlot(int slot) {
        data.setSlot(slot);
    }

    /**
     * Destroys this {@link ItemInstance} from server, and release its objectId.
     *
     * @param process : The identifier of process triggering this action (used by logs).
     * @param actor : The {@link Player} requesting the item destruction.
     * @param reference : The {@link WorldObject} referencing current action like NPC selling item or previous item in
     * transformation.
     */
    public void destroyMe(String process, Player actor, WorldObject reference) {
        setCount(0);
        setOwnerId(process, 0, actor, reference);
        setLocation(ItemLocation.VOID);
        setLastChange(ItemState.REMOVED);

        World.getInstance().removeObject(this);
        IdFactory.getInstance().releaseId(getObjectId());

        if (Config.LOG_ITEMS) {
            final LogRecord record = new LogRecord(Level.INFO, "DELETE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]{
                actor,
                this,
                reference
            });
            ItemFactory.ITEM_LOG.log(record);
        }

        //if (isSummonItem()) {
        //    ItemDao.removePetItem(this);
        //}
    }

    public void setDropperObjectId(int dropperObjectId) {
        data.setDropperObjectId(dropperObjectId);
    }

    @Override
    public void sendInfo(Player player) {
        if (data.getDropperObjectId() != 0) {
            player.sendPacket(new DropItem(this, data.getDropperObjectId()));
        } else {
            player.sendPacket(new SpawnItem(this));
        }
    }

    public List<Quest> getQuestEvents() {
        return getItem().getQuestEvents();
    }

    @Override
    public boolean isChargedShot(ShotType type) {
        return (shotsMask & type.getMask()) == type.getMask();
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        if (charged) {
            shotsMask |= type.getMask();
        } else {
            shotsMask &= ~type.getMask();
        }
    }

    public void unChargeAllShots() {
        shotsMask = 0;
    }

    @Override
    public int compareTo(ItemInstance item) {
        final int time = Long.compare(item.getData().getTime(), data.getTime());
        if (time != 0) {
            return time;
        }

        return Integer.compare(item.getObjectId(), getObjectId());
    }

    public int getDurabilityPercent() {
        int value = -1;
        DurabilityModule durabilityModule = getModule(DurabilityModule.class);
        if (durabilityModule == null) {
            return value;
        }

        value = durabilityModule.getDurabilityPercent();
        return value;
    }

}