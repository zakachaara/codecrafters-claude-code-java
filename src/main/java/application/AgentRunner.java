package application;

import chat.ChatClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import utils.Skills;
import utils.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public final class AgentRunner {
    private static final int MAX_ITERATIONS = 100;

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final Skills skills;

    public AgentRunner(ChatClient chatClient, ToolRegistry toolRegistry, Skills skills) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.skills = skills;
    }

    public void run(String prompt) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(skills.getPrompt())
                        .build()));
        addUserMessages(messages, prompt);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            var response = chatClient.complete(messages, toolRegistry.definitions());
            if (response.choices().isEmpty()) {
                throw new IllegalStateException("no choices in response");
            }

            var assistantMessage = response.choices().get(0).message();
            messages.add(ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder()
                            .content(assistantMessage.content().orElse(""))
                            .toolCalls(assistantMessage._toolCalls())
                            .build()));

            if (assistantMessage.toolCalls().isEmpty()) {
                System.out.print(assistantMessage.content().orElse(""));
                return;
            }

            for (var toolCall : assistantMessage.toolCalls().get()) {
                var function = toolCall.function();
                String result = toolRegistry.execute(function.name(), function.arguments());
                messages.add(ChatCompletionMessageParam.ofTool(
                        ChatCompletionToolMessageParam.builder()
                                .content(result)
                                .toolCallId(toolCall.id())
                                .build()));
            }
        }

        throw new IllegalStateException("agent exceeded maximum iterations");
    }

    private void addUserMessages(List<ChatCompletionMessageParam> messages, String prompt) {
        List<String> stackedSkills = skills.getStackedSkills(prompt);
        if (stackedSkills.isEmpty()) {
            messages.add(userMessage(prompt));
            return;
        }

        for (String skill : stackedSkills) {
            String head = skills.getSkillHeadPrompt(skill);
            String body = skills.substituteInPrompt(prompt, skill);
            messages.add(userMessage(head + body));
        }
    }

    private ChatCompletionMessageParam userMessage(String content) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(content).build());
    }
}
