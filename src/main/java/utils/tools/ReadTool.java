package utils.tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.Map;
import java.util.stream.Collectors;

public class ReadTool extends ChatTool{
    private String jsonSchema = """
                {
                  "type": "object",
                  "properties": {
                    "file_path": {
                      "type": "string",
                      "description": "The path to the file to read"
                    }
                  },
                  "required": ["file_path"]
                }
                """;
    private FunctionDefinition ReadFunction ;
    public ReadTool(){
        try {
            if (ReadFunction != null) {
                return;
            }
            Map<String, Object> schema = mapFields(jsonSchema);


            FunctionParameters params = FunctionParameters.builder()
                    .putAllAdditionalProperties(
                            schema.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> JsonValue.from(e.getValue())
                                    ))
                    )
                    .build();


            this.ReadFunction = FunctionDefinition.builder()
                    .name("Read").description("Read and return the content of a file")
                    .parameters(params)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FunctionDefinition getReadFunction() {
        return this.ReadFunction;
    }

}
