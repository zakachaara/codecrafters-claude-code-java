package utils.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import lsp.LSPClient;
import lsp.LspDocumentSynchronizer;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class LSPTool implements Tool {
    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["goToDefinition" , "findReferences"]
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
    private final LspDocumentSynchronizer documentSynchronizer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LSPTool(LSPClient lspClient, LspDocumentSynchronizer documentSynchronizer) {
        this.lspClient = lspClient;
        this.documentSynchronizer = documentSynchronizer;
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("LSP")
                .description("Returns the file, line, and character where a symbol is defined or referenced.")
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
        Path filePath = Path.of(requiredString(arguments, "filePath"))
                .toAbsolutePath()
                .normalize();
        int line = requiredPosition(arguments, "line");
        int character = requiredPosition(arguments, "character");
        
        switch (operation) {
            case "goToDefinition" -> {
                return goToDefinition(filePath, line - 1, character - 1);
            }
            case "findReferences" -> {
                return findReferences(filePath, line - 1, character - 1);
            }
            default -> throw new IllegalArgumentException("Unknown LSP operation: " + operation);
        }
        
    }

    private synchronized String goToDefinition(
            Path filePath,
            int requestLine,
            int requestCharacter) throws Exception {
        URI documentUri = filePath.toUri();
        documentSynchronizer.ensureOpen(filePath);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", "textDocument/definition");
        ObjectNode params = request.putObject("params");
        params.putObject("textDocument").put("uri", documentUri.toString());
        ObjectNode position = params.putObject("position");
        position.put("line", requestLine);
        position.put("character", requestCharacter);

        String response = lspClient.request(objectMapper.writeValueAsString(request));
        JsonNode result = responseBody(response);
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

    private synchronized String findReferences(
            Path filePath,
            int requestLine,
            int requestCharacter) throws Exception {
        URI documentUri = filePath.toUri();
        documentSynchronizer.ensureOpen(filePath);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", "textDocument/references");
        ObjectNode params = request.putObject("params");
        params.putObject("textDocument").put("uri", documentUri.toString());
        ObjectNode position = params.putObject("position");
        position.put("line", requestLine);
        position.put("character", requestCharacter);
        params.put("context", objectMapper.createObjectNode().put("includeDeclaration", true));
        String response = lspClient.request(objectMapper.writeValueAsString(request));
        JsonNode results = responseBody(response);
        if (!results.isArray() || results.isEmpty()) {
            return "No references found.";
        }

        Map<String, ArrayList<String>> referencesByFile = new LinkedHashMap<>();
        int referenceCount = 0;
        for (JsonNode result : results) {
            String targetUri = result.path("uri").asText(null);
            JsonNode start = result.path("range").path("start");
            if (targetUri == null || start.isMissingNode()) {
                continue;
            }

            Path targetPath = Path.of(URI.create(targetUri))
                    .toAbsolutePath()
                    .normalize();
            Path workingDirectory = Path.of("").toAbsolutePath().normalize();
            String relativePath = workingDirectory.relativize(targetPath)
                    .toString()
                    .replace('\\', '/');
            referenceCount++;

            referencesByFile.computeIfAbsent(relativePath, k -> new ArrayList<>()).add(String.format(
                    "Line %d:%d%n",
                    start.path("line").asInt() + 1,
                    start.path("character").asInt() + 1));
        }
        // construct a string with all references found, or a message if none were found;
        StringBuilder str = new StringBuilder();
        str.append(String.format("Found %d references across %d files:%n", referenceCount, referencesByFile.size()));
        for (Map.Entry<String, ArrayList<String>> entry : referencesByFile.entrySet()) {
            str.append(String.format("%s:%n", entry.getKey()));
            for (String reference : entry.getValue()) {
                str.append(String.format("  %s", reference));
            }
            str.append(System.lineSeparator());
        }
        return str.toString();
    }

    private JsonNode responseBody(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode wrappedResult = root.get("result");
        return wrappedResult == null || wrappedResult.isMissingNode()
                ? root
                : wrappedResult;
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
