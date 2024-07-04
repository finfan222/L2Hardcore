package net.sf.l2j.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author finfan
 */
public class ResourceUtil {

    public static <T> List<T> fromJson(String path, Class<T[]> type, ObjectMapper mapper) {
        try (FileReader reader = new FileReader(path)) {
            return Arrays.asList(mapper.readValue(reader, type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> fromJson(String path, Class<T[]> type) {
        return fromJson(path, type, new ObjectMapper());
    }

}
