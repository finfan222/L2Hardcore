package net.sf.l2j.gameserver.model.mastery.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import net.sf.l2j.gameserver.model.mastery.MasteryHandler;

import java.io.IOException;

/**
 * @author finfan
 */
public class MasteryHandlerDeserializer extends JsonDeserializer<MasteryHandler> {

    @Override
    @SneakyThrows
    public MasteryHandler deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String text = node.asText();
        Class<?> type = Class.forName("net.sf.l2j.gameserver.model.mastery.handlers." + text);
        return (MasteryHandler) type.getConstructor().newInstance();
    }

}
