package net.sf.l2j.commons.eventbus;

import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

/**
 * @author m0nster
 */
@RequiredArgsConstructor
class PipeFilter<Input> implements IEventPipe<Input, Input> {

    private final Predicate<Input> predicate;

    @Override
    public Input process(Input object) {
        return predicate.test(object) ? object : null;
    }

}
