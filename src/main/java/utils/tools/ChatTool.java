package utils.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionParameters;

import java.util.Map;
import java.util.stream.Collectors;

public class ChatTool {
    public static Map<String , Object> mapFields(String jsonSchema) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> schema =
                mapper.readValue(jsonSchema, new TypeReference<Map<String, Object>>() {});
        return schema;
    }

    public static FunctionParameters buildFunctionParameters(Map<String, Object> schema) {
        FunctionParameters params = FunctionParameters.builder()
                .putAllAdditionalProperties(
                        schema.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> JsonValue.from(e.getValue())
                                ))
                )
                .build();
        return params;
    }
}
