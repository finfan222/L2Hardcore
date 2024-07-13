package net.sf.l2j.gameserver.model.mastery;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author finfan
 */
@Data
@RequiredArgsConstructor
public class VariableData {

    private final String key;
    private final Object value;

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

}
