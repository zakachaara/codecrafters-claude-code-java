package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.util.Map;

public class LSPTool extends ChatTool{
    private String jsonSchema = """
                {
                   "type": "object",
                   "properties": {
                     "operation": {
                       "type": "string",
                       "enum": ["goToDefinition"]
                     },
                     "filePath": {
                       "type": "string",
                       "description": "Absolute path, or a path relative to the agent's current working directory."
                     },
                     "line": {
                       "type": "integer",
                       "minimum": 1,
                       "description": "1-based line number of the symbol."
                     },
                     "character": {
                       "type": "integer",
                       "minimum": 1,
                       "description": "1-based character offset in UTF-16 code units (same as character count for ASCII)."
                     }
                   },
                   "required": ["operation", "filePath", "line", "character"]
                 }
                """;
    private FunctionDefinition LSPFunction ;
    public LSPTool(){
        try {
            if (LSPFunction != null) {
                return;
            }
            Map<String, Object> schema = mapFields(jsonSchema);

            FunctionParameters params = buildFunctionParameters(schema);

            this.LSPFunction = FunctionDefinition.builder()
                    .name("LSP").description("Returns the file, line, and character where a symbol is defined.")
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FunctionDefinition getLSPFunction() {
        return this.LSPFunction;
    }


}
