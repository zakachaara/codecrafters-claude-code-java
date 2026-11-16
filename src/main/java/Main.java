import application.AgentRunner;
import application.SubAgentRunner;
import chat.ChatClient;
import config.AppConfig;
import utils.Skills;
import utils.tools.ToolRegistry;

import java.util.Arrays;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        try {
            AppConfig config = AppConfig.fromEnvironment();
            ChatClient chatClient = new ChatClient(config);
            Skills skills = new Skills();
            SubAgentRunner subAgentRunner = new SubAgentRunner(chatClient);
            ToolRegistry toolRegistry = new ToolRegistry(skills, subAgentRunner::run);

            new AgentRunner(chatClient, toolRegistry, skills).run(prompt);
        } catch (Exception exception) {
            System.err.println("Unable to run the agent: " + exception.getMessage());
        }
    }
}
