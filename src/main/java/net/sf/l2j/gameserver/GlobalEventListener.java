package net.sf.l2j.gameserver;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.eventbus.AbstractEventSubscription;
import net.sf.l2j.commons.eventbus.EventBus;
import net.sf.l2j.gameserver.events.EventSituation;
import net.sf.l2j.gameserver.events.OnBuyShopItem;
import net.sf.l2j.gameserver.model.buylist.Product;
import net.sf.l2j.gameserver.taskmanager.BuyListTaskManager;

/**
 * @author finfan
 */
@Slf4j
public class GlobalEventListener {

    private static final EventBus LISTENER = new EventBus();

    public static void initialize() {
        log.info("Global Event Listener was initialized");
        register(OnBuyShopItem.class).forEach(GlobalEventListener::onBuyShopItem);
    }

    public static <EventType extends EventSituation> AbstractEventSubscription<EventType> register(Class<EventType> type) {
        return LISTENER.subscribe().cast(type);
    }

    public static <T extends EventSituation> void notify(T event) {
        LISTENER.notify(event);
    }

    private static void onBuyShopItem(OnBuyShopItem event) {
        Product product = event.getProduct();
        if (product.hasLimitedStock()) {
            BuyListTaskManager.getInstance().add(product);
            log.debug("{} x{} was bought by {} from {} with totalPrice={}", product, event.getCount(),
                event.getBuyer(), event.getNpc(), event.getTotalPrice());
        }
    }

}
