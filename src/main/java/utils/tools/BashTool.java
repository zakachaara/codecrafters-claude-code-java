package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import utils.Response.CommandResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public final class BashTool implements Tool {
    private static final String JSON_SCHEMA = """
                {
                   "type": "object",
                   "required": ["command"],
                   "properties": {
                     "command": {
                       "type": "string",
                       "description": "The command to execute"
                     }
                   }
                 }
                """;
    private final FunctionDefinition definition;

    public BashTool() {
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("Bash").description("Execute a shell command")
                .parameters(params)
                .build();
    }

    @Override
    public String name() {
        return "Bash";
    }

    @Override
    public FunctionDefinition definition() {
        return definition;
    }

    @Override
    public String execute(Map<String, Object> arguments) throws Exception {
        Object command = arguments.get("command");
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        Process process = new ProcessBuilder("bash", "-c", command.toString()).start();
        String message;
        String errorMessage;
        try {
            message = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            errorMessage = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            CommandResult result = new CommandResult(exitCode, message, errorMessage);
            return result.getCode() == 0 ? result.getMessage() : result.getError();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            process.destroy();
        }
    }
}
