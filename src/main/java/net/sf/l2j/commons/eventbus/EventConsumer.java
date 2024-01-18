package net.sf.l2j.commons.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотированный метод будет подписан на аргумент метода в {@link EventBus#GLOBAL глобальной шине сообщений}.
 *
 * @author m0nster
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventConsumer {
}
