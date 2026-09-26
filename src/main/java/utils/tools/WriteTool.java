package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.util.Map;

public class WriteTool extends ChatTool{
    private String jsonSchema = """
                {
                   "type": "object",
                   "required": ["file_path", "content"],
                   "properties": {
                     "file_path": {
                       "type": "string",
                       "description": "The path of the file to write to"
                     },
                     "content": {
                       "type": "string",
                       "description": "The content to write to the file"
                     }
                   }
                 }
                """;
    private FunctionDefinition WriteFunction ;
    public WriteTool(){
        try {
            if (WriteFunction != null) {
                return;
            }
            Map<String, Object> schema = mapFields(jsonSchema);

            FunctionParameters params = buildFunctionParameters(schema);

            this.WriteFunction = FunctionDefinition.builder()
                    .name("Write").description("Write content to a file")
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FunctionDefinition getWriteFunction() {
        return this.WriteFunction;
    }
}
