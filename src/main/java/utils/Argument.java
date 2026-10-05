package utils;

import java.util.Arrays;

public class Argument {
    String[] allArgs = null;
    String[] shortArgs = null;
    String[] longArgs = null;

    // Passing the arguments
    String[] argsWord = null;
    final String cmd;
    final String oldPrompt;

    public Argument(String cmd ,String body , String prompt , String[] args){
        this.cmd = cmd;
        this.oldPrompt = prompt;

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
        // $ARGUMENTS[n]
        System.out.println("this is the original prompt in substituteArgs "+prompt);
        if (longArgs.length > 0) {
            for (String arg : longArgs) {
                int start = arg.indexOf('[') + 1;
                int end = arg.indexOf(']');
                int argIndex = Integer.parseInt(
                        arg.substring(start, end)
                );
                String substitutor =
                        argIndex < argsWord.length
                                ? argsWord[argIndex]
                                : "";
                newPrompt = newPrompt.replace(arg, substitutor);
            }
        }
        // $ARGUMENTS
        if (allArgs.length > 0) {
            String arguments = String.join(" ", argsWord);
            newPrompt = newPrompt.replace(
                    "$ARGUMENTS",
                    arguments
            );
        }
        // $0, $1, etc.
        if (shortArgs.length > 0) {
            for (String arg : shortArgs) {
                int argIndex = Integer.parseInt(arg.substring(1));
                String substitutor =
                        argIndex < argsWord.length
                                ? argsWord[argIndex]
                                : "";
                newPrompt = newPrompt.replace(
                        arg,
                        substitutor
                );
            }
        }
        System.out.println("this is the new prompt after substituteArgs "+newPrompt);
        return newPrompt;
    }



}
