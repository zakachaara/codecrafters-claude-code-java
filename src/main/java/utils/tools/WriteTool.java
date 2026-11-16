package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import lsp.LspDocumentSynchronizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class WriteTool implements Tool {
    private static final String JSON_SCHEMA = """
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
    private final FunctionDefinition definition;
    private final LspDocumentSynchronizer documentSynchronizer;

    public WriteTool(LspDocumentSynchronizer documentSynchronizer) {
        this.documentSynchronizer = documentSynchronizer;
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("Write").description("Write content to a file")
                .parameters(params)
                .build();
    }

    @Override
    public String name() {
        return "Write";
    }

    @Override
    public FunctionDefinition definition() {
        return definition;
    }

    @Override
    public String execute(Map<String, Object> arguments) throws Exception {
        Object filePath = arguments.get("file_path");
        Object content = arguments.get("content");
        if (filePath == null || content == null) {
            throw new IllegalArgumentException("file_path and content are required");
        }
        Path path = Path.of(filePath.toString());
        String savedContent = content.toString();
        Files.writeString(path, savedContent);
        documentSynchronizer.synchronizeWrite(path, savedContent);
        return savedContent;
    }
}
