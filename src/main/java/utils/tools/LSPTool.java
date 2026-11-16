package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.util.Map;

public final class LSPTool implements Tool {
    private static final String JSON_SCHEMA = """
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
                  "description": "1-based character offset in UTF-16 code units."
                }
              },
              "required": ["operation", "filePath", "line", "character"]
            }
            """;

    private final FunctionDefinition definition;

    public LSPTool() {
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("LSP")
                .description("Returns the file, line, and character where a symbol is defined.")
                .parameters(params)
                .build();
    }

    @Override
    public String name() {
        return "LSP";
    }

    @Override
    public FunctionDefinition definition() {
        return definition;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        return "LSP is not implemented yet";
    }
}
