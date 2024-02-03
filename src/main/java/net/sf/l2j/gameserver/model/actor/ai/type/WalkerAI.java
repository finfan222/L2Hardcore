package net.sf.l2j.gameserver.model.actor.ai.type;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Walker;
import net.sf.l2j.gameserver.model.location.WalkerLocation;
import net.sf.l2j.gameserver.taskmanager.WalkerTaskManager;

import java.util.List;

/**
 * This AI is used by {@link Walker}s.<br>
 * <br>
 * We use basic functionalities of AI, notably {@link IntentionType} MOVE_TO and onEvtArrived() to motion it.<br> It
 * retains a List of {@link WalkerLocation}s which, together, creates a route, and we save the current node index to
 * find the next WalkerLocation. Once the path is complete, we return to index 0.<br>
 * <br>
 * It is associated to a global task named {@link WalkerTaskManager} to handle individual WalkerLocation delays.
 */
@Slf4j
public class WalkerAI extends CreatureAI {

    private int routeIndex;

    public WalkerAI(Creature creature) {
        super(creature);
    }

    @Override
    public Walker getActor() {
        return (Walker) _actor;
    }

    @Override
    protected void onEvtArrived() {
        // Retrieve walker route, if any.
        final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(getActor().getNpcId());
        if (route == null || route.isEmpty()) {
            return;
        }

        // Retrieve current node.
        final WalkerLocation node = route.get(routeIndex);

        if (node.getChat() != null) {
            getActor().broadcastNpcSay(node.getChat());
        }

        // We freeze the NPC and store it on WalkerTaskManager, which will release it in the future.
        if (node.getDelay() > 0) {
            WalkerTaskManager.getInstance().add(getActor(), node.getDelay());
        } else {
            moveToNextPoint();
        }
    }

    /**
     * Move the {@link Walker} to the next {@link WalkerLocation} of his route.
     */
    public void moveToNextPoint() {
        // Retrieve walker route, if any.
        final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(getActor().getNpcId());
        if (route == null || route.isEmpty()) {
            return;
        }

        // Set the next node value.
        if (routeIndex < route.size() - 1) {
            routeIndex++;
        } else {
            routeIndex = 0;
        }

        // Retrieve next node.
        final WalkerLocation node;
        try {
            node = route.get(routeIndex);
        } catch (Exception e) {
            log.error("Bug occurred in walker routes. NpcId={}, maxRoutes={}, currentIndex={}", getActor().getNpcId(), route.size() - 1, routeIndex, e);
            throw new RuntimeException(e);
        }

        // Running state.
        if (node.mustRun()) {
            getActor().forceRunStance();
        } else {
            getActor().forceWalkStance();
        }

        doMoveToIntention(node, null);
    }
}