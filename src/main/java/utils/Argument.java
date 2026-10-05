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

    public String substituteArgs(String prompt){
        String newPrompt = prompt;

        // substitute longArgs . $ARGUMENTS[1],
        if (longArgs.length > 0) {

            for (String arg : longArgs) {
                String substitutor ;
                // get index
                int argIndex = Integer.parseInt(arg.substring(arg.indexOf('[')+1,arg.indexOf(']')));

                if (argIndex < argsWord.length) {
                    substitutor = argsWord[argIndex];
                }else substitutor = "";

                newPrompt = newPrompt.replace(arg, substitutor);

            }
        }

        // substitue allArgs , $ARGUMENTS only;
        if (allArgs.length > 0) {
            String arguments = String.join(" ", argsWord);
            newPrompt = newPrompt.replace("$ARGUMENTS", arguments);
        }


        // substitute shortArgs , $X;
        if (shortArgs.length > 0) {

            for (String arg : shortArgs) {
                String substitutor ;
                // get index
                int argIndex = Integer.parseInt(arg.substring(1));

                if (argIndex < argsWord.length) {
                    substitutor = argsWord[argIndex];
                }else substitutor = "";

                newPrompt = newPrompt.replace(arg, substitutor);

            }
        }

        return newPrompt;
    }


}
