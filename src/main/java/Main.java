import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        // tool list
        List<ChatCompletionTool> tools = new ArrayList<>();
        // Advertising Function tool ; Read tool
        // 1- Function Parameters

        String jsonSchema = """
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
        try {
            Map<String, Object> schema = mapFields(jsonSchema);


        FunctionParameters params = FunctionParameters.builder()
                .putAllAdditionalProperties(
                        schema.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> JsonValue.from(e.getValue())
                                ))
                )
                .build();


        FunctionDefinition ReadFunction = 	FunctionDefinition.builder()
                .name("Read").description("Read and return the content of a file")
                .parameters(params)
                .build();

        // Add tools to the list
        tools.add(ChatCompletionTool.builder()
                .type(JsonValue.from("function")).function(ReadFunction)
                .build()
        );

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model("anthropic/claude-haiku-4.5")
                        .addUserMessage(prompt)
                        .tools(tools)
                        .build()
        );

        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

            if (response.choices().get(0).message()._toolCalls().isMissing()){
                System.out.print(response.choices().get(0).message().content().orElse(""));
            }else {
                // Extract Tool Call Parameters
                Map<String , JsonValue> toolCall = response.choices().get(0).message()._toolCalls().asObject().get();
                Map<String , JsonValue> function = toolCall.entrySet().stream().filter(x -> x.getKey().equals("function"))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> JsonValue.from(e.getValue())
                        ));
                String functionName = function.get("name").toString();
                String functionArgs = function.get("arguments").toString();
                // Extract the Tool Call Arguments from the json string;
                Map<String, Object> parsedArgs = mapFields(functionArgs);
                // Execute the Read Tool Call
                if (functionName.equals("Read") ){
                    String filePath = parsedArgs.get("file_path").toString();
                    Path path = Paths.get(filePath);
                    BufferedReader br = Files.newBufferedReader(path);
                    br.lines().forEach(System.out::println);
                    br.close();
                }

            }



        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String , Object> mapFields(String jsonSchema) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> schema =
                mapper.readValue(jsonSchema, new TypeReference<Map<String, Object>>() {});
        return schema;
    }
}
