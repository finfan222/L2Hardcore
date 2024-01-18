package net.sf.l2j.commons.eventbus;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * @author m0nster
 */
@RequiredArgsConstructor
class PipeConsumer<Input> implements IEventPipe<Input, Input> {

    private final Consumer<Input> consumer;

    @Override
    public Input process(Input object) {
        consumer.accept(object);
        return object;
    }

}
