package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import utils.Response.CommandResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class BashTool extends ChatTool{
    private String jsonSchema = """
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
    private FunctionDefinition BashFunction ;
    public BashTool(){
        try {
            if (BashFunction != null) {
                return;
            }
            Map<String, Object> schema = mapFields(jsonSchema);

            FunctionParameters params = buildFunctionParameters(schema);

            this.BashFunction = FunctionDefinition.builder()
                    .name("Bash").description("Execute a shell command")
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FunctionDefinition getBashFunction() {
        return this.BashFunction;
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    Process process ;

    public CommandResult execute(String command) {
        try {

            processBuilder.command("bash", "-c", command);
            process = processBuilder.start();
            int exitCode = process.waitFor();
            var input = process.getInputStream();
            var error = process.getErrorStream();

            String message = new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));
            String errorMessage = new BufferedReader(new InputStreamReader(error)).lines().collect(Collectors.joining("\n"));

            return new CommandResult(exitCode, message, errorMessage);

        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return new CommandResult(0, "", "");
    }
}
