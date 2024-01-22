package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.graveyard.DieReason;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnDie implements EventSituation {

    private Creature victim;
    private Creature killer;
    private DieReason reason;

}
