package utils.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import lsp.LSPClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final LSPClient lspClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<URI> openedDocuments = new HashSet<>();

    public LSPTool(LSPClient lspClient) {
        this.lspClient = lspClient;
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
    public String execute(Map<String, Object> arguments) throws Exception {
        if (lspClient == null) {
            throw new IllegalStateException("LSP client is not configured");
        }

        String operation = requiredString(arguments, "operation");
        if (!"goToDefinition".equals(operation)) {
            throw new IllegalArgumentException("Unknown LSP operation: " + operation);
        }

        Path filePath = Path.of(requiredString(arguments, "filePath"))
                .toAbsolutePath()
                .normalize();
        int line = requiredPosition(arguments, "line");
        int character = requiredPosition(arguments, "character");

        return goToDefinition(filePath, line - 1, character - 1);
    }

    private synchronized String goToDefinition(
            Path filePath,
            int requestLine,
            int requestCharacter) throws Exception {
        URI documentUri = filePath.toUri();
        ensureDocumentOpen(filePath, documentUri);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", "textDocument/definition");
        ObjectNode params = request.putObject("params");
        params.putObject("textDocument").put("uri", documentUri.toString());
        ObjectNode position = params.putObject("position");
        position.put("line", requestLine);
        position.put("character", requestCharacter);

        String response = lspClient.request(objectMapper.writeValueAsString(request));
        JsonNode result = objectMapper.readTree(response).path("result");
        if (result.isMissingNode() || result.isNull()) {
            throw new IllegalStateException("LSP returned no definition");
        }
        if (result.isArray()) {
            if (result.isEmpty()) {
                throw new IllegalStateException("LSP returned no definition");
            }
            result = result.get(0);
        }

        String targetUri = result.path("uri").asText(null);
        JsonNode start = result.path("range").path("start");
        if (targetUri == null || start.isMissingNode()) {
            throw new IllegalStateException("Invalid LSP definition response");
        }

        Path targetPath = Path.of(URI.create(targetUri))
                .toAbsolutePath()
                .normalize();
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        String relativePath = workingDirectory.relativize(targetPath)
                .toString()
                .replace('\\', '/');

        return String.format(
                "Defined in %s:%d:%d",
                relativePath,
                start.path("line").asInt() + 1,
                start.path("character").asInt() + 1);
    }

    private void ensureDocumentOpen(Path filePath, URI documentUri) throws Exception {
        if (openedDocuments.contains(documentUri)) {
            return;
        }

        String extension = extensionOf(filePath);
        String language = lspClient.mapExtensionToLanguage(extension);
        if (language == null) {
            throw new IllegalArgumentException("No LSP language mapping for " + extension);
        }

        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("method", "textDocument/didOpen");
        ObjectNode textDocument = notification.putObject("params")
                .putObject("textDocument");
        textDocument.put("uri", documentUri.toString());
        textDocument.put("languageId", language);
        textDocument.put("version", 1);
        textDocument.put("text", Files.readString(filePath));

        lspClient.notify(objectMapper.writeValueAsString(notification));
        openedDocuments.add(documentUri);
    }

    private String extensionOf(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("File has no extension: " + fileName);
        }
        return fileName.substring(dot);
    }

    private String requiredString(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return string;
    }

    private int requiredPosition(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof Number number) || number.intValue() < 1) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
        return number.intValue();
    }
}
