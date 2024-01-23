package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnValidatePosition implements EventSituation {

    private Location position;

}
