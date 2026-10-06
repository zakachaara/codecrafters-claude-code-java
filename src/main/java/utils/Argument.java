package utils;

import java.util.Arrays;

public class Argument {
    String[] allArgs = null;
    String[] shortArgs = null;
    String[] longArgs = null;

    // Passing the arguments
    String[] argsWord = null;
    final String cmd;

    public Argument(String cmd ,String body , String[] args){
        this.cmd = cmd;

        String[] argsPlaceholder = Arrays.stream(body.split("\\s+"))
                .filter(word -> word.startsWith("$"))
                .toArray(String[]::new);
        this.shortArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.length() < 10).toArray(String[]::new);
        this.longArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.length() > 10).toArray(String[]::new);
        this.allArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.equals("$ARGUMENTS")).toArray(String[]::new);

        this.argsWord = args;
    }

    public String substituteArgs(String prompt) {
        String newPrompt = prompt;
        String arguments = String.join(" ", argsWord);
        // $ARGUMENTS
        newPrompt = newPrompt.replace(
                "$ARGUMENTS",
                arguments
        );

        // $ARGUMENTS[n]
        for (int i = 0; i < argsWord.length; i++) {
            newPrompt = newPrompt.replace(
                    "$ARGUMENTS[" + i + "]",
                    argsWord[i]
            );
        }

        // $0, $1, $2, ...
        for (int i = 0; i < argsWord.length; i++) {
            newPrompt = newPrompt.replace(
                    "$" + i,
                    argsWord[i]
            );
        }
        return newPrompt;
    }




}
