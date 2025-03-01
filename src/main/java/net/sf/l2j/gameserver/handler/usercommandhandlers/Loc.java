package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Loc implements IUserCommandHandler {
    private static final int[] COMMAND_IDS =
        {
            0
        };

    @Override
    public void useUserCommand(int id, Player player) {
        SystemMessageId msg;

        switch (MapRegionData.getInstance().getMapRegion(player.getX(), player.getY())) {
            case 0:
                msg = SystemMessageId.LOC_TI_S1_S2_S3;
                break;

            case 1:
                msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
                break;

            case 2:
                msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
                break;

            case 3:
                msg = SystemMessageId.LOC_ORC_S1_S2_S3;
                break;
            case 4:
                msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
                break;

            case 5:
                msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
                break;

            case 6:
                msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
                break;

            case 7:
                msg = SystemMessageId.LOC_DION_S1_S2_S3;
                break;

            case 8:
                msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
                break;

            case 9:
                msg = SystemMessageId.LOC_OREN_S1_S2_S3;
                break;

            case 10:
                msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
                break;

            case 11:
                msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
                break;

            case 12:
                msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
                break;

            case 13:
                msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
                break;

            case 14:
                msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
                break;

            case 15:
                msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
                break;

            case 16:
                msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
                break;

            case 17:
                msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
                break;

            case 18:
                msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
                break;

            default:
                msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
        }

        player.sendPacket(SystemMessage.getSystemMessage(msg).addNumber(player.getX()).addNumber(player.getY()).addNumber(player.getZ()));
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}