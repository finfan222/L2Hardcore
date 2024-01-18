package net.sf.l2j.commons.eventbus;

/**
 * @author m0nster
 */
public interface IEventPipe<Input, Output> {

    Output process(Input object);

}
