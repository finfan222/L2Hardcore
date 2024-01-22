package net.sf.l2j.gameserver.model.graveyard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostScript {

    private String name;
    private String message;
    private DieReason reason;
    private int x;
    private int y;
    private int z;
    private int heading;
    private LocalDate date;
    private boolean isEternal;

}
