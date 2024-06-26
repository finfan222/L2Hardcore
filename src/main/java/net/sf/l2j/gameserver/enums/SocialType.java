package net.sf.l2j.gameserver.enums;

import lombok.Getter;

/**
 * @author finfan
 */
@Getter
public enum SocialType {
    HELLO(2),
    VICTORY(3),
    CHARGE(4),
    NO(5),
    YES(6),
    BOW(7),
    THINK(8),
    WARMUP(9),
    LAUGHT(10),
    CLAP(11),
    DANCE(12),
    SADNESS(13),
    LEVEL_UP(15),
    HERO_UP(17),
    ;

    private final int id;

    SocialType(int id) {
        this.id = id;
    }
}
