package net.sf.l2j.gameserver.model.graveyard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author finfan
 */
@Getter
@RequiredArgsConstructor
public enum DieReason {

    NONE(false, "погиб по неизвестным причинам", "death from unknown cause"),
    PVP(false, "погиб в бою против игрока", "died in battle against a player"),
    @Deprecated
    PK(false, "получил тяжёлое ранение от жестокого разбойника из банды PK", "was seriously injured by a brutal robber from the PK gang"),
    DROWN(true, "утонул в водах", "drowned"),
    GUARD(true, "нарушил закон, за что и поплатился", "broke the law and paid for it"),
    MONSTER(true, "погиб в битве, от рук", "died in battle against monster(s)"),
    FALL(true, "погиб от падения с большой высоты", "died from a fall from a great height"),
    MORTAL_COMBAT(true, "погиб в смертельной битве", "died in a mortal combat");

    private final boolean isHardcoreDeath;
    private final String diePartMessage;
    private final String dieWorldMessage;

}
