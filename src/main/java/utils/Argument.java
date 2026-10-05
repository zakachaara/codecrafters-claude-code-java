package utils;

import java.util.Arrays;

public class Argument {
    String[] allArgs = null;
    String[] shortArgs = null;
    String[] longArgs = null;

    // Passing the arguments
    String[] argsWord = null;
    String cmd = null;
    String oldPrompt = null;

    public Argument(String cmd ,String body , String prompt){
        this.cmd = cmd;
        this.oldPrompt = prompt;

        String[] argsPlaceholder = Arrays.stream(body.split("\\s+"))
                .filter(word -> word.startsWith("$"))
                .toArray(String[]::new);
        this.shortArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.length() < 10).toArray(String[]::new);
        this.longArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.length() > 10).toArray(String[]::new);
        this.allArgs = Arrays.stream(argsPlaceholder).filter(arg -> arg.equals("$ARGUMENTS")).toArray(String[]::new);

        if (prompt.equals(cmd)){
            this.argsWord = Arrays.stream(prompt.substring(cmd.length()).split("\\s+"))
                    .toArray(String[]::new);
        }else {
            this.argsWord = Arrays.stream(prompt.substring(cmd.length() + 1).split("\\s+"))
                    .toArray(String[]::new);
        }
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
            newPrompt = newPrompt.replace(allArgs[0] , this.oldPrompt.substring(cmd.length()+1));
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
