package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IAdminCommandHandler {
    Logger log = LoggerFactory.getLogger(IAdminCommandHandler.class.getName());

    int PAGE_LIMIT_7 = 7;
    int PAGE_LIMIT_8 = 8;
    int PAGE_LIMIT_10 = 10;
    int PAGE_LIMIT_15 = 15;
    int PAGE_LIMIT_18 = 18;
    int PAGE_LIMIT_20 = 20;

    void useAdminCommand(String command, Player player);

    String[] getAdminCommandList();

    default Player getTargetPlayer(Player player, String playerName, boolean defaultAdmin) {
        final Player toTest = World.getInstance().getPlayer(playerName);
        return (toTest == null) ? getTargetPlayer(player, defaultAdmin) : toTest;
    }

    default Player getTargetPlayer(Player player, boolean defaultAdmin) {
        return getTarget(Player.class, player, defaultAdmin);
    }

    default Creature getTargetCreature(Player player, boolean defaultAdmin) {
        return getTarget(Creature.class, player, defaultAdmin);
    }

    /**
     * @param <A> : The {@link Class} to cast upon result.
     * @param type : The {@link Class} type to check.
     * @param player : The {@link Player} used to retrieve the target from.
     * @param defaultAdmin : If true, we test the {@link Player} itself, in case target was invalid, otherwise we return
     * null directly.
     * @return The target of the {@link Player} set as parameter, under the given {@link Class} type. If the target
     * isn't assignable to that {@link Class}, or if the defaultAdmin is set to true and the {@link Player} instance
     * isn't assignable to that {@link Class} aswell, then return null.
     */
    @SuppressWarnings("unchecked")
    default <A> A getTarget(Class<A> type, Player player, boolean defaultAdmin) {
        final WorldObject target = player.getTarget();

        // Current player target is null or not assignable, return either himself (if type was assignable to Player) or null.
        if (target == null || !type.isAssignableFrom(target.getClass())) {
            return (defaultAdmin && type.isAssignableFrom(player.getClass())) ? (A) player : null;
        }

        return (A) target;
    }

    default void sendFile(Player player, String filename) {
        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/admin/" + filename);
        player.sendPacket(html);
    }
}