package net.sf.l2j.gameserver.model.cards;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity {

    private int objectId;
    private int slotId;
    private int classIndex;
    private int symbolId;

    public CardData getData() {
        return CardManager.getInstance().get(symbolId);
    }
}
