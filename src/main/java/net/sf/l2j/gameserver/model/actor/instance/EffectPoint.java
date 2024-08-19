package net.sf.l2j.gameserver.model.actor.instance;

import lombok.Getter;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

@Getter
public class EffectPoint extends Npc {

    private final Creature owner;

    public EffectPoint(int objectId, NpcTemplate template, Creature owner) {
        super(objectId, template);
        this.owner = (owner == null) ? null : owner.getActingPlayer();
    }

    @Override
    public Player getActingPlayer() {
        return owner instanceof Player ? (Player) owner : null;
    }

    @Override
    public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed) {
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    @Override
    public boolean hasRandomAnimation() {
        return false;
    }

    @Override
    public boolean isAttackableBy(Creature attacker) {
        return false;
    }
}