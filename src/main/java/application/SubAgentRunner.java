package application;

import chat.ChatClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import java.util.ArrayList;
import java.util.List;

public final class SubAgentRunner {
    private final ChatClient chatClient;

    public SubAgentRunner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String run(String prompt) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(prompt).build()));

        var response = chatClient.completeWithoutTools(messages);
        if (response.choices().isEmpty()) {
            throw new IllegalStateException("no choices in response");
        }

        var message = response.choices().get(0).message();
        if (!message.toolCalls().isEmpty()) {
            throw new IllegalStateException("Tool calls are not supported for sub agents");
        }
        return message.content().orElse("");
    }
}
