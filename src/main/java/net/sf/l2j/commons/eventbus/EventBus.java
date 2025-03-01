package net.sf.l2j.commons.eventbus;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author m0nster
 */
public class EventBus {

    public static final EventBus GLOBAL = new EventBus();

    private final Set<AbstractEventSubscription<?>> subscriptions = ConcurrentHashMap.newKeySet();

    @SuppressWarnings({
        "rawtypes",
        "unchecked"
    })
    public void notify(Object object) {
        for (AbstractEventSubscription subscription : subscriptions) {
            subscription.execute(object);
        }
    }

    public <T> AbstractEventSubscription<T> subscribe() {
        final SingleEventSubscription<T> subscription = new SingleEventSubscription<>();
        subscriptions.add(subscription);
        return subscription;
    }

    public <T> void unsubscribe(AbstractEventSubscription<T> subscription) {
        subscriptions.remove(subscription);
    }

    public <T> void unsubscribe(Object object) {
        subscriptions.removeIf(e -> Objects.equals(e.getGroup(), object));
    }

    public <T> AbstractEventSubscription<T> subscribe(AbstractEventSubscription<T> subscription) {
        subscriptions.add(subscription);
        return subscription;
    }

}
