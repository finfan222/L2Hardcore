package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;

public class CharInfo extends L2GameServerPacket {
    private final Player player;

    public CharInfo(Player player) {
        this.player = player;
    }

    @Override
    protected final void writeImpl() {
        boolean canSeeInvis = false;

        if (!player.getAppearance().isVisible()) {
            final Player tmp = getClient().getPlayer();
            if (tmp != null && tmp.isGM()) {
                canSeeInvis = true;
            }
        }

        writeC(0x03);
        writeD(player.getX());
        writeD(player.getY());
        writeD(player.getZ());
        writeD((player.getBoat() == null) ? 0 : player.getBoat().getObjectId());
        writeD(player.getObjectId());
        writeS(player.getName());
        writeD(player.getRace().ordinal());
        writeD(player.getAppearance().getSex().ordinal());
        writeD((player.getClassIndex() == 0) ? player.getClassId().getId() : player.getBaseClass());

        writeD(player.getInventory().getItemIdFrom(Paperdoll.HAIRALL));
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

        writeD(player.getPvpFlag());
        writeD(player.getKarma());
        writeD(player.getStatus().getMAtkSpd());
        writeD(player.getStatus().getPAtkSpd());
        writeD(player.getPvpFlag());
        writeD(player.getKarma());

        final int runSpd = player.getStatus().getBaseRunSpeed();
        final int walkSpd = player.getStatus().getBaseWalkSpeed();
        final int swimSpd = player.getStatus().getBaseSwimSpeed();

        writeD(runSpd);
        writeD(walkSpd);
        writeD(swimSpd);
        writeD(swimSpd);
        writeD(runSpd);
        writeD(walkSpd);
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

        writeS((canSeeInvis) ? "Invisible" : player.getTitle());

        writeD(player.getClanId());
        writeD(player.getClanCrestId());
        writeD(player.getAllyId());
        writeD(player.getAllyCrestId());

        writeD(0);

        writeC((player.isSitting()) ? 0 : 1);
        writeC((player.isRunning()) ? 1 : 0);
        writeC((player.isInCombat()) ? 1 : 0);
        writeC((player.isAlikeDead()) ? 1 : 0);
        writeC((!canSeeInvis && !player.getAppearance().isVisible()) ? 1 : 0);

        writeC(player.getMountType());
        writeC(player.getOperateType().getId());

        writeH(player.getCubicList().size());
        for (final Cubic cubic : player.getCubicList()) {
            writeH(cubic.getId());
        }

        writeC((player.isInPartyMatchRoom()) ? 1 : 0);
        writeD((canSeeInvis) ? (player.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : player.getAbnormalEffect());
        writeC(player.getRecomLeft());
        writeH(player.getRecomHave());
        writeD(player.getClassId().getId());
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
        writeD(player.getHeading());
        writeD(player.getPledgeClass());
        writeD(player.getPledgeType());
        writeD(player.getAppearance().getTitleColor());
        writeD(CursedWeaponManager.getInstance().getCurrentStage(player.getCursedWeaponEquippedId()));
    }
}