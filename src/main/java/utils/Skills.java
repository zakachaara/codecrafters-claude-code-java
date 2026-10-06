package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Skills {
    private Map<String , SkillFormat> skill_map = new HashMap<>();
    private String prompt;
    private Map<String , String> skill_prompt = new HashMap<>();

    public Skills() {
        // load .claude/skills and determine skill paths
        Path skillsPath = Path.of(".claude", "skills");
        List<String> skillsPaths = loadSkillsPathFromFolder(skillsPath);

        // loop over all the paths in it and read every Skill.md metadata
        this.skill_map = readSkills(skillsPaths);
        // Build the System prompt ;
        this.prompt = buildPrompt();
    }

    private List<String> loadSkillsPathFromFolder(Path path) {
        ArrayList<String> skillsPathList = new ArrayList<>();

        File[] files = path.toFile().listFiles();

        if (files == null) {
            return skillsPathList;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                Path skillPath = file.toPath().resolve("SKILL.md");
                skillsPathList.add(skillPath.toString());
            }
        }

        return skillsPathList;
    }


    private Map<String, SkillFormat> readSkills(List<String> paths) {
        Map<String, SkillFormat> skill_map = new HashMap<>();

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

                skill_map.put(skill.getName(), skill);
                skill_prompt.put(
                        "/"+skill.getName(),
                        body.toString()
                );
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to read skill: " + filePath, e
                );
            }
        }

        return skill_map;
    }

    private String buildPrompt(){
        StringBuilder prompt = new StringBuilder(
                "You have access to the following skills:\n\n"
        );

        for (SkillFormat skill : skill_map.values()) {
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
        return skill_prompt.get(command);
    }
    public boolean skillExists(String skill){
        return skill_prompt.containsKey(skill);
    }

    private boolean requiresLevel3Context(String skill) {
        String skillBody = getSkillPrompt(skill);
        return skillBody.contains("/scripts")
                || skillBody.contains("/references")
                || skillBody.contains("/assets");
    }
    public boolean isSubAgented(String skill) {
        String context = skill_map.get(skill).getContext();
        return context.equals("fork");
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

    List<String> stacked ;
    String[] newArgs;

    public void extractStackedSkills(String oldPrompt) {
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
        this.newArgs = Arrays.copyOfRange(stack, indexArgs, stack.length);
        this.stacked = stacked;
    }


    public List<String> getStackedSkills(String oldPrompt) {
        if (stacked == null) {
            this.extractStackedSkills(oldPrompt);
        }
        return stacked;
    }

    public String subsituteInPrompt(String oldPrompt, String cmd){
        String skillPrompt = this.getSkillPrompt(cmd);

        String newPrompt = cmd;

        if (skillPrompt != null) {
            // check for Arguments
            if (newArgs == null){
                this.extractStackedSkills(oldPrompt);
            }
            Argument arg = new Argument(cmd , skillPrompt , this.newArgs);

            // substitue allArgs ;
            newPrompt = arg.substituteArgs(skillPrompt);
        }
        return newPrompt;
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
