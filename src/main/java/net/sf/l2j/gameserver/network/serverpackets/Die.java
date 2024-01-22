package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.entity.ClanHallSiege;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.graveyard.DieReason;
import net.sf.l2j.gameserver.model.pledge.Clan;

public class Die extends L2GameServerPacket {

    private static final int SHOW = 0x01;
    private static final int NOT_SHOW = 0x00;

    private final Creature victim;
    private final int objectId;
    private final boolean isFakeDeath;

    private boolean isSweepable;
    private boolean isAllowFixedRes;
    private Clan clan;
    private DieReason dieReason;

    public Die(Creature victim) {
        this.victim = victim;
        this.objectId = victim.getObjectId();
        this.isFakeDeath = !victim.isDead();

        if (victim instanceof Player player) {
            this.clan = player.getClan();
            this.isAllowFixedRes = player.getAccessLevel().allowFixedRes();
            this.isSweepable = player.getSpoilState().isSweepable();
            this.dieReason = player.getDieReason();
        } else if (victim instanceof Monster monster) {
            this.isSweepable = monster.getSpoilState().isSweepable();
        }
    }

    @Override
    protected final void writeImpl() {
        if (isFakeDeath) {
            return;
        }

        writeC(0x06);
        writeD(objectId);
        if (!dieReason.isHardcoreDeath()) {
            writeD(SHOW); // to nearest village
            if (clan != null) {
                final Siege siege = CastleManager.getInstance().getActiveSiege(victim);
                final ClanHallSiege chs = ClanHallManager.getInstance().getActiveSiege(victim);

                // Check first if an active Siege is under process.
                if (siege != null) {
                    final SiegeSide side = siege.getSide(clan);

                    writeD((clan.hasClanHall()) ? SHOW : NOT_SHOW); // to clanhall
                    writeD((clan.hasCastle() || side == SiegeSide.OWNER || side == SiegeSide.DEFENDER) ? SHOW : NOT_SHOW); // to castle
                    writeD((side == SiegeSide.ATTACKER && clan.getFlag() != null) ? SHOW : NOT_SHOW); // to siege HQ
                }
                // If no Siege, check ClanHallSiege.
                else if (chs != null) {
                    writeD((clan.hasClanHall()) ? SHOW : NOT_SHOW); // to clanhall
                    writeD((clan.hasCastle()) ? SHOW : NOT_SHOW); // to castle
                    writeD((chs.checkSide(clan, SiegeSide.ATTACKER) && clan.getFlag() != null) ? SHOW : NOT_SHOW); // to siege HQ
                }
                // We're in peace mode, activate generic teleports.
                else {
                    writeD((clan.hasClanHall()) ? SHOW : NOT_SHOW); // to clanhall
                    writeD((clan.hasCastle()) ? SHOW : NOT_SHOW); // to castle
                    writeD(NOT_SHOW); // to siege HQ
                }
            } else {
                writeD(NOT_SHOW); // to clanhall
                writeD(NOT_SHOW); // to castle
                writeD(NOT_SHOW); // to siege HQ
            }
        } else {
            writeD(NOT_SHOW); // to nearest village
            writeD(NOT_SHOW); // to clanhall
            writeD(NOT_SHOW); // to castle
            writeD(NOT_SHOW); // to siege HQ
        }

        writeD((isSweepable) ? SHOW : NOT_SHOW); // sweepable (blue glow)
        writeD((isAllowFixedRes) ? SHOW : NOT_SHOW); // FIXED
    }
}