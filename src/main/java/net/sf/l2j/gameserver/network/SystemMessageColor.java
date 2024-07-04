package net.sf.l2j.gameserver.network;

import lombok.Getter;

/**
 * @author finfan
 */
@Getter
public enum SystemMessageColor {

    DEFAULT(SystemMessageId.S1),
    ORANGE_LIGHT(SystemMessageId.S1_ORANGE_LIGHT),
    GREEN_LIGHT(SystemMessageId.S1_GREEN_LIGHT),
    BLUE_LIGHT(SystemMessageId.S1_BLUE_LIGHT),
    RED_LIGHT(SystemMessageId.S1_RED_LIGHT),
    GREY_LIGHT(SystemMessageId.S1_GREY_LIGHT);

    private final SystemMessageId id;

    SystemMessageColor(SystemMessageId id) {
        this.id = id;
    }

}
