package net.sf.l2j.commons.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Результат вызова метода будет передан в {@link EventBus#GLOBAL глобальную шину сообщений}.
 *
 * @author m0nster
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventProducer {

}
