package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import utils.Skills;

import java.util.Map;
import java.util.function.Function;

public final class SkillTool implements Tool {
    private static final String JSON_SCHEMA = """
                    {
                      "type": "object",
                      "required": ["name"],
                      "properties": {
                        "name": { "type": "string", "description": "The name of the skill to use" },
                        "args": { "type": "string", "description": "Optional arguments for the skill" }
                      }
                    }
                    """;
    private final FunctionDefinition definition;
    private final Skills skills;
    private final Function<String, String> subAgentRunner;

    public SkillTool(Skills skills, Function<String, String> subAgentRunner) {
        this.skills = skills;
        this.subAgentRunner = subAgentRunner;
        Map<String, Object> schema = ChatTool.mapFields(JSON_SCHEMA);
        FunctionParameters params = ChatTool.buildFunctionParameters(schema);
        this.definition = FunctionDefinition.builder()
                .name("Skill").description("Load a skill's instructions into the conversation")
                .parameters(params)
                .build();
    }

    @Override
    public String name() {
        return "Skill";
    }

    @Override
    public FunctionDefinition definition() {
        return definition;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        Object name = arguments.get("name");
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }

        String skillName = name.toString();
        String skillArgs = arguments.containsKey("args")
                ? arguments.get("args").toString()
                : "";
        String skillMessage = skills.getSkillToolPrompt(skillName, skillArgs);

        if (skills.isSubAgented(skillName)) {
            skillMessage = "Skill " + skillName
                    + " ran in a separate context and returned:"
                    + subAgentRunner.apply(skillMessage);
        }
        return skillMessage;
    }
}
