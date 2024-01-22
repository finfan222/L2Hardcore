package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.graveyard.DieReason;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnDieLethal implements EventSituation {

    private String name;
    private DieReason reason;

}
