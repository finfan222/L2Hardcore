package net.sf.l2j.commons.eventbus;

/**
 * @author m0nster
 */
@SuppressWarnings({
    "rawtypes",
    "unchecked"
})
public class SingleEventSubscription<T> extends AbstractEventSubscription<T> {

    @Override
    void execute(T object) {
        Object input = object;
        for (int i = 0; i < pipe.size() && input != null; i++) {
            final IEventPipe pipe = this.pipe.get(i);
            input = pipe.process(input);
        }
    }

}
