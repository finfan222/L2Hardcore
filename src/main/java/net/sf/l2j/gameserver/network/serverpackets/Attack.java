package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.attack.CreatureAttack.HitHolder;

public class Attack extends L2GameServerPacket {
    public static final int HITFLAG_USESS = 0x10;
    public static final int HITFLAG_CRIT = 0x20;
    public static final int HITFLAG_SHLD = 0x40;
    public static final int HITFLAG_MISS = 0x80;

    private final int _attackerId;
    public final boolean soulshot;
    public final int _ssGrade;
    private final int _x;
    private final int _y;
    private final int _z;
    private HitHolder[] _hits;

    public Attack(Creature attacker, boolean useShots, int ssGrade) {
        _attackerId = attacker.getObjectId();
        soulshot = useShots;
        _ssGrade = ssGrade;
        _x = attacker.getX();
        _y = attacker.getY();
        _z = attacker.getZ();
    }

    public boolean processHits(HitHolder[] hits) {
        _hits = hits;

        boolean isHit = false;

        for (HitHolder hit : hits) {
            if (hit.isMissed) {
                hit.flags = HITFLAG_MISS;
                continue;
            }

            isHit = true;

            if (soulshot) {
                hit.flags = HITFLAG_USESS | _ssGrade;
            }

            if (hit.isCritical) {
                hit.flags |= HITFLAG_CRIT;
            }

            if (hit.block != ShieldDefense.FAILED) {
                hit.flags |= HITFLAG_SHLD;
            }
        }
        return isHit;
    }

    /**
     * @return True if this {@link Attack} serverpacket is filled.
     */
    public boolean hasHits() {
        return _hits != null;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x05);

        writeD(_attackerId);
        writeD(_hits[0].targetId);
        writeD(_hits[0].damage);
        writeC(_hits[0].flags);
        writeD(_x);
        writeD(_y);
        writeD(_z);
        writeH(_hits.length - 1);

        if (_hits.length > 1) {
            for (int i = 1; i < _hits.length; i++) {
                writeD(_hits[i].targetId);
                writeD(_hits[i].damage);
                writeC(_hits[i].flags);
            }
        }
    }
}