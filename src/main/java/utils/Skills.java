package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Skills {
    private final Map<String, SkillFormat> skillMap;
    private final String prompt;
    private final Map<String, String> skillPrompts = new LinkedHashMap<>();

    public Skills() {
        // load .claude/skills and determine skill paths
        Path skillsPath = Path.of(".claude", "skills");
        List<String> skillsPaths = loadSkillsPathFromFolder(skillsPath);

        // loop over all the paths in it and read every Skill.md metadata
        this.skillMap = readSkills(skillsPaths);
        // Build the System prompt ;
        this.prompt = buildPrompt();
    }

    private List<String> loadSkillsPathFromFolder(Path path) {
        ArrayList<String> skillsPathList = new ArrayList<>();

        try (var files = Files.list(path)) {
            files.filter(Files::isDirectory)
                    .map(directory -> directory.resolve("SKILL.md"))
                    .filter(Files::exists)
                    .sorted()
                    .map(Path::toString)
                    .forEach(skillsPathList::add);
        } catch (IOException ignored) {
            return skillsPathList;
        }

        return skillsPathList;
    }


    private Map<String, SkillFormat> readSkills(List<String> paths) {
        Map<String, SkillFormat> skills = new LinkedHashMap<>();

        for (String filePath : paths) {
            Path path = Paths.get(filePath);
            SkillFormat skill = new SkillFormat();

            try (BufferedReader br = Files.newBufferedReader(path)) {
                int markerCount = 0;
                String line;
                StringBuilder body = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    // Frontmatter markers
                    if (line.startsWith("---")) {
                        markerCount++;
                        continue;
                    }
                    // We are inside the frontmatter
                    if (markerCount < 2) {
                        if (line.startsWith("name:")) {
                            skill.setName(
                                    line.substring("name:".length()).trim()
                            );
                        }
                        else if (line.startsWith("description:")) {
                            skill.setDescription(
                                    line.substring("description:".length()).trim()
                            );
                        } else if (line.startsWith("context:")) {
                            skill.setContext(
                                    line.substring("context:".length()).trim()
                            );
                        }
                        continue;
                    }
                    // We are now inside the actual skill body
                    body.append(line).append("\n");
                }

                skills.put(skill.getName(), skill);
                skillPrompts.put(
                        "/"+skill.getName(),
                        body.toString()
                );
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to read skill: " + filePath, e
                );
            }
        }

        return skills;
    }

    private String buildPrompt(){
        StringBuilder prompt = new StringBuilder(
                "You have access to the following skills:\n\n"
        );

        for (SkillFormat skill : skillMap.values()) {
            prompt.append(skill).append("\n");
        }
        prompt.append("If a skill matches the user's request, call the Skill tool with its name and follow the instructions it returns.");
        return prompt.toString();
    }

    public String getPrompt() {
        return prompt;
    }
    private String getSkillPrompt(String command){
        // get the prompt of the command from the hashmap
        return skillPrompts.get(command);
    }
    public boolean skillExists(String skill){
        return skillPrompts.containsKey(skill);
    }

    private boolean requiresLevel3Context(String skill) {
        String skillBody = getSkillPrompt(skill);
        if (skillBody == null) {
            return false;
        }
        return skillBody.contains("/scripts")
                || skillBody.contains("/references")
                || skillBody.contains("/assets");
    }
    public boolean isSubAgented(String skill) {
        SkillFormat skillFormat = skillMap.get(skill);
        return skillFormat != null && "fork".equals(skillFormat.getContext());
    }

    public String getSkillHeadPrompt(String skill){
        if (!requiresLevel3Context(skill)) {
            return "";
        }
        StringBuilder prompt = new StringBuilder("Skill: ");
        prompt.append(skill.substring(1)+ " (located at .claude/skills"+skill+")\n");
        prompt.append("Paths in the instructions below are relative to that folder.\n");

        return prompt.toString();
    }

    public List<String> extractStackedSkills(String oldPrompt) {
        String[] stack = oldPrompt.trim().split("\\s+");
        List<String> stacked = new ArrayList<>();
        int indexArgs = stack.length;
        for (int i = 0; i < stack.length; i++) {
            String word = stack[i];
            if (word.startsWith("/") && this.skillExists(word)) {
                stacked.add(word);
            } else {
                indexArgs = i;
                break;
            }
        }
        return stacked;
    }

    public List<String> getStackedSkills(String oldPrompt) {
        return extractStackedSkills(oldPrompt);
    }

    public String substituteInPrompt(String oldPrompt, String cmd){
        String skillPrompt = this.getSkillPrompt(cmd);

        String newPrompt = cmd;

        if (skillPrompt != null) {
            String[] stack = oldPrompt.trim().split("\\s+");
            int indexArgs = stack.length;
            for (int i = 0; i < stack.length; i++) {
                if (!stack[i].startsWith("/") || !this.skillExists(stack[i])) {
                    indexArgs = i;
                    break;
                }
            }
            String[] arguments = Arrays.copyOfRange(stack, indexArgs, stack.length);
            Argument arg = new Argument(cmd, skillPrompt, arguments);

            newPrompt = arg.substituteArgs(skillPrompt);
        }
        return newPrompt;
    }

    @Deprecated
    public String subsituteInPrompt(String oldPrompt, String cmd) {
        return substituteInPrompt(oldPrompt, cmd);
    }

    public String getSkillToolPrompt(String skillName , String args){
        String skill = "/"+skillName;
        if(this.skillExists(skill)){
            String skillBody = this.getSkillPrompt(skill);
            if (args == null) {
                args = "";
            }
            String[] arguments = args.split("\\s+");
            Argument toolArg = new Argument(skill, skillBody , arguments);

            String newBody = toolArg.substituteArgs(skillBody);

            return newBody;
        }
        return "This tool does not exist.";
    }
}
