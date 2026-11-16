package utils.tools;

import com.openai.models.FunctionDefinition;

import java.util.Map;

public interface Tool {
    String name();

    FunctionDefinition definition();

    String execute(Map<String, Object> arguments) throws Exception;
}
