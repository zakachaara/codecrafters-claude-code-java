package utils.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ChatTool {
    public static Map<String , Object> mapFields(String jsonSchema) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> schema =
                mapper.readValue(jsonSchema, new TypeReference<Map<String, Object>>() {});
        return schema;
    }
}
