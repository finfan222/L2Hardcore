package net.sf.l2j.gameserver.network.serverpackets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExShowScreenMessageCustom extends L2GameServerPacket {

    public enum Align {
        DUMMY,
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        MIDDLE_RIGHT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
    }

    @Builder.Default
    private int type = 0x01; // 0 - system messages, 1 - your defined text
    @Builder.Default
    private int msgId = -1; // system message id (_type must be 0 otherwise no effect)
    @Builder.Default
    private int unk2 = 0x00;
    @Builder.Default
    private int unk3 = 0x00;
    @Builder.Default
    private int fontSize = 0x00; // font size 0 - normal, 1 - small
    @Builder.Default
    private int time = 2500; // time

    @Builder.Default
    private boolean showHide = false; // hide
    @Builder.Default
    private boolean showFading = false; // fade effect (0 - disabled, 1 enabled)
    @Builder.Default
    private boolean showEffect = false; // upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect

    @Builder.Default
    private Align align = Align.TOP_CENTER; // message position

    @Builder.Default
    private String text = ""; // your text (_type must be 1, otherwise no effect)

    @Override
    protected void writeImpl() {
        writeC(0xfe);
        writeH(0x38);
        writeD(type);
        writeD(msgId);
        writeD(align.ordinal());
        writeD(showHide ? 1 : 0);
        writeD(fontSize);
        writeD(unk2); // ?
        writeD(unk3); // ?
        writeD(showEffect ? 1 : 0);
        writeD(time);
        writeD(showFading ? 1 : 0);
        writeS(text);
    }
}