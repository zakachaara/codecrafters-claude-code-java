package lsp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LspDocumentSynchronizer {
    private final LSPClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<URI, Integer> documentVersions = new HashMap<>();
    private final Set<URI> openedDocuments = new HashSet<>();

    public LspDocumentSynchronizer(LSPClient client) {
        this.client = client;
    }

    public synchronized void ensureOpen(Path path) throws Exception {
        if (client == null) {
            throw new IllegalStateException("LSP client is not configured");
        }

        Path normalizedPath = normalize(path);
        URI uri = normalizedPath.toUri();
        if (openedDocuments.contains(uri)) {
            return;
        }

        openDocument(normalizedPath, uri, Files.readString(normalizedPath));
    }

    public synchronized void synchronizeWrite(Path path, String content) throws Exception {
        if (client == null) {
            return;
        }

        Path normalizedPath = normalize(path);
        URI uri = normalizedPath.toUri();
        String language = languageFor(normalizedPath);
        if (language == null) {
            return;
        }

        if (!openedDocuments.contains(uri)) {
            openDocument(normalizedPath, uri, content);
            return;
        }

        int nextVersion = documentVersions.get(uri) + 1;
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("method", "textDocument/didChange");
        ObjectNode params = notification.putObject("params");
        params.putObject("textDocument")
                .put("uri", uri.toString())
                .put("version", nextVersion);
        params.putArray("contentChanges")
                .addObject()
                .put("text", content);

        client.notify(objectMapper.writeValueAsString(notification));
        documentVersions.put(uri, nextVersion);
    }

    private void openDocument(Path path, URI uri, String content) throws Exception {
        String language = languageFor(path);
        if (language == null) {
            throw new IllegalArgumentException("No LSP language mapping for " + extensionOf(path));
        }

        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("method", "textDocument/didOpen");
        ObjectNode textDocument = notification.putObject("params")
                .putObject("textDocument");
        textDocument.put("uri", uri.toString());
        textDocument.put("languageId", language);
        textDocument.put("version", 1);
        textDocument.put("text", content);

        client.notify(objectMapper.writeValueAsString(notification));
        openedDocuments.add(uri);
        documentVersions.put(uri, 1);
    }

    private String languageFor(Path path) {
        String extension = extensionOf(path);
        return extension == null ? null : client.mapExtensionToLanguage(extension);
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? null : fileName.substring(dot);
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
