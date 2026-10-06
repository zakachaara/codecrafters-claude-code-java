package utils.tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;

import java.util.Map;

public class SkillTool extends ChatTool{
    private String jsonSchema = """
                    {
                      "type": "object",
                      "required": ["name"],
                      "properties": {
                        "name": { "type": "string", "description": "The name of the skill to use" },
                        "args": { "type": "string", "description": "Optional arguments for the skill" }
                      }
                    }
                    """;
    private FunctionDefinition skillFunction ;
    public SkillTool(){
        try {
            if (skillFunction != null) {
                return;
            }
            Map<String, Object> schema = mapFields(jsonSchema);

            FunctionParameters params = buildFunctionParameters(schema);

            this.skillFunction = FunctionDefinition.builder()
                    .name("Skill").description("Load a skill's instructions into the conversation")
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FunctionDefinition getSkillFunction() {
        return skillFunction;
    }
}
