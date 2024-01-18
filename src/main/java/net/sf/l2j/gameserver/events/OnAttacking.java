package net.sf.l2j.gameserver.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.attack.CreatureAttack;

/**
 * @author finfan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnAttacking<Attacker extends Creature> implements EventSituation {

    private Attacker attacker;
    private Creature target;
    private CreatureAttack<Attacker> attack;

}
