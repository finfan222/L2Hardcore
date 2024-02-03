package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.enums.DayCycle;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnDayCycleChange implements EventSituation {

    private DayCycle current;
    private DayCycle previous;

}
