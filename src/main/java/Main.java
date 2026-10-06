import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.*;
import utils.Response.CommandResult;
import utils.Skills;
import utils.tools.BashTool;
import utils.tools.ChatTool;
import utils.tools.ReadTool;
import utils.tools.WriteTool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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


        try{
            FunctionDefinition ReadFunction = new ReadTool().getReadFunction();
            FunctionDefinition WriteFunction = new WriteTool().getWriteFunction();
            FunctionDefinition BashFunction = new BashTool().getBashFunction();

            // Add tools to the list
            // ==1== Read Tool
            tools.add(ChatCompletionTool.builder()
                .type(JsonValue.from("function")).function(ReadFunction)
                .build()
            );
            // ==2== Write Tool
            tools.add(ChatCompletionTool.builder()
                    .type(JsonValue.from("function")).function(WriteFunction)
                    .build()
            );
            // ==3== Bash Tool
            tools.add(ChatCompletionTool.builder()
                    .type(JsonValue.from("function")).function(BashFunction)
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
            // Store Messages :
            List<ChatCompletionMessageParam> messages = new ArrayList<>();

            // Advice Skills :
            Skills skills = new Skills();

            ChatCompletionMessageParam skillParam = ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                            .content(skills.getPrompt()).build());

            messages.add(skillParam);

            // -- First message : User Prompt
            // look for skills and change them with the right prompt;
            List<String> stackedSkills = skills.getStackedSkills(prompt) ;
            if (stackedSkills.isEmpty()) {
                // put the original prompt in .
                ChatCompletionMessageParam messageParam = ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(prompt).build());

                messages.add(messageParam);
            }

            for(String skill : stackedSkills) {
                String newPromptHead = skills.getSkillHeadPrompt(skill);
                String newPrompt = skills.subsituteInPrompt(prompt , skill);

                // put the new prompt in .
                ChatCompletionMessageParam messageParam = ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(newPromptHead+newPrompt).build());

                messages.add(messageParam);
            }

            // start loop using while : stop iteration after no tool called
            ChatCompletion response ;

            while(true){
                // call api on messages
                response = client.chat().completions().create(
                        ChatCompletionCreateParams.builder()
                                .model("anthropic/claude-haiku-4.5")
                                .messages(messages)
                                .tools(tools)
                                .build()
                );

                if (response.choices().isEmpty()) {
                    throw new RuntimeException("no choices in response");
                }
                // get the last response choice message
                var lastmessage = response.choices().get(0).message();

                // Record the Assistant Responce Message
                messages.add(ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                                .content(lastmessage.content().orElse(""))
                                .toolCalls(lastmessage._toolCalls())
                                .build()
                ));

                // Executing Tool Calls : Looping over tool calls
                if (!lastmessage.toolCalls().isEmpty()) {
                    for (var toolCall : lastmessage.toolCalls().get()){

                        var functionCall = toolCall.function();

                        String functionName = functionCall.name();
                        String functionArgs = functionCall.arguments();
                        String toolCallID = toolCall.id();
                        Map<String, Object> parsedArgs = ChatTool.mapFields(functionArgs);

                        if ("Read".equals(functionName)) {

                            String filePath = parsedArgs.get("file_path").toString();

                            Path path = Paths.get(filePath);
                            String fileContent ;
                            try (BufferedReader br = Files.newBufferedReader(path)) {
                                fileContent = br.lines()
                                        .collect(Collectors.joining(System.lineSeparator()));
                            }
                            messages.add(ChatCompletionMessageParam.ofTool(
                                    ChatCompletionToolMessageParam.builder()
                                            .content(fileContent)
                                            .toolCallId(toolCallID)
                                            .build()
                            ));

                        } else if ("Write".equals(functionName)) {
                            String filePath = parsedArgs.get("file_path").toString();
                            String fileContent = parsedArgs.get("content").toString();

                            try{
                                Path path = Paths.get(filePath);
                                // create the file if it does not exist
                                BufferedWriter bw = Files.newBufferedWriter(path);
                                bw.write(fileContent);
                                bw.close();

                                // Append the result to messages :
                                messages.add(ChatCompletionMessageParam.ofTool(
                                        ChatCompletionToolMessageParam.builder()
                                                .content(fileContent)
                                                .toolCallId(toolCallID)
                                                .build()
                                ));

                            } catch (IOException e) {

                            }

                        } else if ("Bash".equals(functionName)) {
                            String command = parsedArgs.get("command").toString();
                            String executionResult = null;
                            try {

                                CommandResult res = new BashTool().execute(command);
                                if (res.getCode() == 0){
                                    executionResult = res.getMessage();
                                }else{
                                    executionResult = res.getError();
                                }
                                // Append the result to messages :
                                messages.add(ChatCompletionMessageParam.ofTool(
                                        ChatCompletionToolMessageParam.builder()
                                                .content(executionResult)
                                                .toolCallId(toolCallID)
                                                .build()
                                ));

                            }catch (Exception e){

                            }

                        }
                    }
                }else {
                    // Print out the last message response
                    System.out.print(lastmessage.content().orElse(""));
                    break; // No more work to do
                }

            }

            } catch (IOException ex) {

        } catch (Exception ex) {

        }


    }


}
