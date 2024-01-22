package net.sf.l2j.gameserver.enums;

import lombok.Getter;

/**
 * @author finfan
 */
@Getter
public enum HtmColor {
    YELLOW("FFFF00"),
    DARK_YELLOW("#CCCC00"),
    RED("FF0000"),
    GREEN("00FF00"),
    CYAN("00FFFF"),
    WHITE("000000"),
    BLACK("FFFFFF");

    private final String code;

    HtmColor(String code) {
        this.code = code;
    }

    public String asColored(String text) {
        return "<font color=" + code + ">" + text + "</font>";
    }
}
