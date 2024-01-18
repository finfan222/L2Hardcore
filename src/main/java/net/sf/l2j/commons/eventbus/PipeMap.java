package net.sf.l2j.commons.eventbus;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * @author m0nster
 */
@RequiredArgsConstructor
class PipeMap<Input, Output> implements IEventPipe<Input, Output> {

    private final Function<Input, Output> function;

    @Override
    public Output process(Input object) {
        return function.apply(object);
    }

}
