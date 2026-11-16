package lsp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public final class LspDiagnosticsTracker {
    private final LSPClient client;
    private final LspDocumentSynchronizer documentSynchronizer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<URI, Set<DiagnosticKey>> shownDiagnostics = new HashMap<>();
    private int notificationCursor;

    public LspDiagnosticsTracker(
            LSPClient client,
            LspDocumentSynchronizer documentSynchronizer) {
        this.client = client;
        this.documentSynchronizer = documentSynchronizer;
    }

    public synchronized String collectNewDiagnostics() throws Exception {
        if (client == null) {
            return "";
        }

        JsonNode response = objectMapper.readTree(client.notifications(notificationCursor));
        JsonNode wrappedResult = response.get("result");
        if (wrappedResult != null && wrappedResult.isObject()) {
            response = wrappedResult;
        }
        notificationCursor = response.path("cursor").asInt(notificationCursor);

        Map<String, List<DiagnosticLocation>> newDiagnostics = new LinkedHashMap<>();
        for (JsonNode notification : response.path("notifications")) {
            if (!"textDocument/publishDiagnostics".equals(notification.path("method").asText())) {
                continue;
            }

            JsonNode params = notification.path("params");
            URI uri = URI.create(params.path("uri").asText());
            if (!isCurrentVersion(uri, params.get("version"))) {
                continue;
            }

            Set<DiagnosticKey> previous = shownDiagnostics.getOrDefault(uri, Set.of());
            Set<DiagnosticKey> current = new HashSet<>();
            for (JsonNode diagnostic : params.path("diagnostics")) {
                DiagnosticKey key = DiagnosticKey.from(diagnostic);
                current.add(key);
                if (!previous.contains(key)) {
                    String relativePath = relativePath(uri);
                    newDiagnostics
                            .computeIfAbsent(relativePath, ignored -> new ArrayList<>())
                            .add(new DiagnosticLocation(
                                    key.line() + 1,
                                    key.character() + 1,
                                    key.message()));
                }
            }
            shownDiagnostics.put(uri, current);
        }

        return format(newDiagnostics);
    }

    private boolean isCurrentVersion(URI uri, JsonNode versionNode) {
        if (versionNode == null || versionNode.isNull()) {
            return true;
        }

        OptionalInt version = documentSynchronizer.versionFor(uri);
        return version.isPresent() && version.getAsInt() == versionNode.asInt();
    }

    private String relativePath(URI uri) {
        Path target = Path.of(uri).toAbsolutePath().normalize();
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        return workingDirectory.relativize(target)
                .toString()
                .replace('\\', '/');
    }

    private String format(Map<String, List<DiagnosticLocation>> diagnosticsByFile) {
        if (diagnosticsByFile.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(
                "<new-diagnostics>The following new diagnostic issues were detected:\n\n");
        for (Map.Entry<String, List<DiagnosticLocation>> entry : diagnosticsByFile.entrySet()) {
            result.append(entry.getKey()).append(":\n");
            for (DiagnosticLocation diagnostic : entry.getValue()) {
                result.append("  [Line ")
                        .append(diagnostic.line())
                        .append(':')
                        .append(diagnostic.character())
                        .append("] ")
                        .append(diagnostic.message())
                        .append('\n');
            }
        }
        return result.append("</new-diagnostics>").toString();
    }

    private record DiagnosticLocation(int line, int character, String message) {
    }

    private record DiagnosticKey(
            int line,
            int character,
            int endLine,
            int endCharacter,
            int severity,
            String source,
            String message) {

        private static DiagnosticKey from(JsonNode diagnostic) {
            JsonNode start = diagnostic.path("range").path("start");
            JsonNode end = diagnostic.path("range").path("end");
            return new DiagnosticKey(
                    start.path("line").asInt(),
                    start.path("character").asInt(),
                    end.path("line").asInt(),
                    end.path("character").asInt(),
                    diagnostic.path("severity").asInt(0),
                    diagnostic.path("source").asText(""),
                    diagnostic.path("message").asText(""));
        }
    }
}
