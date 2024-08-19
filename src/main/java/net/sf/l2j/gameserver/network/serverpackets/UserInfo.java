package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;

public class UserInfo extends L2GameServerPacket {

    private final Player player;
    private int relation;

    public UserInfo(Player player) {
        this.player = player;
        this.relation = this.player.isClanLeader() ? 0x40 : 0;

        if (this.player.getSiegeState() == 1) {
            relation |= 0x180;
        }
        if (this.player.getSiegeState() == 2) {
            relation |= 0x80;
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x04);
        writeD(player.getX());
        writeD(player.getY());
        writeD(player.getZ());
        writeD(player.getHeading());
        writeD(player.getObjectId());
        writeS((player.getPolymorphTemplate() != null) ? player.getPolymorphTemplate().getName() : player.getName());
        writeD(player.getRace().ordinal());
        writeD(player.getAppearance().getSex().ordinal());
        writeD((player.getClassIndex() == 0) ? player.getClassId().getId() : player.getBaseClass());
        writeD(player.getStatus().getLevel());
        writeQ(player.getStatus().getExp());
        writeD(player.getStatus().getSTR());
        writeD(player.getStatus().getDEX());
        writeD(player.getStatus().getCON());
        writeD(player.getStatus().getINT());
        writeD(player.getStatus().getWIT());
        writeD(player.getStatus().getMEN());
        writeD(player.getStatus().getMaxHp());
        writeD((int) player.getStatus().getHp());
        writeD(player.getStatus().getMaxMp());
        writeD((int) player.getStatus().getMp());
        writeD(player.getStatus().getSp());
        writeD(player.getCurrentWeight());
        writeD(player.getWeightLimit());
        writeD(player.getActiveWeaponItem() != null ? 40 : 20);

        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.HAIRALL));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.REAR));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.LEAR));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.NECK));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.RFINGER));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.LFINGER));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.HEAD));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.RHAND));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.LHAND));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.GLOVES));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.CHEST));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.LEGS));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.FEET));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.CLOAK));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.RHAND));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.HAIR));
        writeD(player.getInventory().getItemObjectIdFrom(Paperdoll.FACE));

        writeD(player.getInventory().getItemIdFrom(Paperdoll.HAIRALL));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.REAR));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.LEAR));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.NECK));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.RFINGER));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.LFINGER));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.HEAD));
        writeD(player.getTwoHandGrip().get() ? player.getActiveWeaponItem().getTwoHandId()
            : player.getInventory().getItemIdFrom(Paperdoll.RHAND));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.LHAND));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.GLOVES));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.CHEST));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.LEGS));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.FEET));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.CLOAK));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.RHAND));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.HAIR));
        writeD(player.getInventory().getItemIdFrom(Paperdoll.FACE));

        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(player.getInventory().getAugmentationIdFrom(Paperdoll.RHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(player.getInventory().getAugmentationIdFrom(Paperdoll.LHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);

        writeD(player.getStatus().getPAtk(null));
        writeD(player.getStatus().getPAtkSpd());
        writeD(player.getStatus().getPDef(null));
        writeD(player.getStatus().getEvasionRate(null));
        writeD(player.getStatus().getAccuracy());
        writeD(player.getStatus().getCriticalHit(null, null));
        writeD(player.getStatus().getMAtk(null, null));
        writeD(player.getStatus().getMAtkSpd());
        writeD(player.getStatus().getPAtkSpd());
        writeD(player.getStatus().getMDef(null, null));
        writeD(player.getPvpFlag());
        writeD(player.getKarma());

        final int runSpd = player.getStatus().getBaseRunSpeed();
        final int walkSpd = player.getStatus().getBaseWalkSpeed();
        final int swimSpd = player.getStatus().getBaseSwimSpeed();

        writeD(runSpd);
        writeD(walkSpd);
        writeD(swimSpd);
        writeD(swimSpd);
        writeD(0);
        writeD(0);
        writeD((player.isFlying()) ? runSpd : 0);
        writeD((player.isFlying()) ? walkSpd : 0);

        writeF(player.getStatus().getMovementSpeedMultiplier());
        writeF(player.getStatus().getAttackSpeedMultiplier());

        final Summon summon = player.getSummon();
        if (player.isMounted() && summon != null) {
            writeF(summon.getCollisionRadius());
            writeF(summon.getCollisionHeight());
        } else {
            writeF(player.getCollisionRadius());
            writeF(player.getCollisionHeight());
        }

        writeD(player.getAppearance().getHairStyle());
        writeD(player.getAppearance().getHairColor());
        writeD(player.getAppearance().getFace());
        writeD((player.isGM()) ? 1 : 0);

        writeS((player.getPolymorphTemplate() != null) ? "Morphed" : player.getTitle());

        writeD(player.getClanId());
        writeD(player.getClanCrestId());
        writeD(player.getAllyId());
        writeD(player.getAllyCrestId());
        writeD(relation);
        writeC(player.getMountType());
        writeC(player.getOperateType().getId());
        writeC((player.hasDwarvenCraft()) ? 1 : 0);
        writeD(player.getPkKills());
        writeD(player.getPvpKills());

        writeH(player.getCubicList().size());
        for (final Cubic cubic : player.getCubicList()) {
            writeH(cubic.getId());
        }

        writeC((player.isInPartyMatchRoom()) ? 1 : 0);
        writeD((!player.getAppearance().isVisible() && player.isGM()) ? (player.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : player.getAbnormalEffect());
        writeC(0x00);
        writeD(player.getClanPrivileges());
        writeH(player.getRecomLeft());
        writeH(player.getRecomHave());
        writeD((player.getMountNpcId() > 0) ? player.getMountNpcId() + 1000000 : 0);
        writeH(player.getStatus().getInventoryLimit());
        writeD(player.getClassId().getId());
        writeD(0x00);
        writeD(player.getStatus().getMaxCp());
        writeD((int) player.getStatus().getCp());
        writeC((player.isMounted()) ? 0 : player.getEnchantEffect());
        writeC((Config.PLAYER_SPAWN_PROTECTION > 0 && player.isSpawnProtected()) ? TeamType.BLUE.getId() : player.getTeam().getId());
        writeD(player.getClanCrestLargeId());
        writeC((player.isNoble()) ? 1 : 0);
        writeC((player.isHero() || (player.isGM() && Config.GM_HERO_AURA)) ? 1 : 0);
        writeC((player.isFishing()) ? 1 : 0);
        writeLoc(player.getFishingStance().getLoc());
        writeD(player.getAppearance().getNameColor());
        writeC((player.isRunning()) ? 0x01 : 0x00);
        writeD(player.getPledgeClass());
        writeD(player.getPledgeType());
        writeD(player.getAppearance().getTitleColor());
        writeD(CursedWeaponManager.getInstance().getCurrentStage(player.getCursedWeaponEquippedId()));
    }

    private void sendRightHandeWriteD() {

    }
}