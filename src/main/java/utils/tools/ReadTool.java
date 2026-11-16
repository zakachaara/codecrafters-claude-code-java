package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ReadTool implements Tool {
    private static final String JSON_SCHEMA = """
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
    private final FunctionDefinition definition;

    public ReadTool() {
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("Read").description("Read and return the content of a file")
                .parameters(params)
                .build();
    }

    @Override
    public String name() {
        return "Read";
    }

    @Override
    public FunctionDefinition definition() {
        return definition;
    }

    @Override
    public String execute(Map<String, Object> arguments) throws Exception {
        Object filePath = arguments.get("file_path");
        if (filePath == null) {
            throw new IllegalArgumentException("file_path is required");
        }
        return Files.readString(Path.of(filePath.toString()));
    }
}
