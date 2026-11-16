package chat;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import config.AppConfig;

import java.util.List;

public final class ChatClient {
    private final OpenAIClient client;
    private final AppConfig config;

    public ChatClient(AppConfig config) {
        this.config = config;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .build();
    }

    public ChatCompletion complete(List<ChatCompletionMessageParam> messages,
                                   List<ChatCompletionTool> tools) {
        return client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(config.model())
                        .messages(messages)
                        .tools(tools)
                        .build());
    }

    public ChatCompletion completeWithoutTools(List<ChatCompletionMessageParam> messages) {
        return client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(config.model())
                        .messages(messages)
                        .build());
    }
}
