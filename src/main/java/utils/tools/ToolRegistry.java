package utils.tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.Optional;

import lsp.LSPClient;
import lsp.LspDiagnosticsTracker;
import lsp.LspDocumentSynchronizer;
import utils.Skills;

public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final LspDiagnosticsTracker diagnosticsTracker;

    public ToolRegistry(Skills skills, Function<String, String> subAgentRunner, Optional<LSPClient> lspClient) {
        LSPClient client = lspClient.orElse(null);
        LspDocumentSynchronizer documentSynchronizer = new LspDocumentSynchronizer(client);
        this.diagnosticsTracker = new LspDiagnosticsTracker(client, documentSynchronizer);
        register(new ReadTool());
        register(new WriteTool(documentSynchronizer));
        register(new BashTool());
        register(new SkillTool(skills, subAgentRunner));
        register(new LSPTool(client, documentSynchronizer));
    }

    private void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public List<ChatCompletionTool> definitions() {
        List<ChatCompletionTool> definitions = new ArrayList<>();
        for (Tool tool : tools.values()) {
            definitions.add(ChatCompletionTool.builder()
                    .type(JsonValue.from("function"))
                    .function(tool.definition())
                    .build());
        }
        return definitions;
    }

    public String execute(String name, String jsonArguments) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return "Unknown tool: " + name;
        }

        try {
            return tool.execute(ChatTool.mapFields(jsonArguments));
        } catch (Exception exception) {
            return "Tool " + name + " failed: " + exception.getMessage();
        }
    }

    public String collectNewDiagnostics() throws Exception {
        return diagnosticsTracker.collectNewDiagnostics();
    }
}
