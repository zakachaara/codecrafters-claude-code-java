package utils;

import utils.tools.SkillFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Skills {
    List<SkillFormat> skill_list = new ArrayList<>();
    String prompt;
    Map<String , String> skill_prompt = new HashMap<>();

    public Skills() {
        // load .claude/skills and determine skill paths
        Path skillsPath = Path.of(".claude", "skills");
        List<String> skillsPaths = loadSkillsPathFromFolder(skillsPath);

        // loop over all the paths in it and read every Skill.md metadata
        this.skill_list = readSkills(skillsPaths);
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


    private List<SkillFormat> readSkills(List<String> paths) {
        List<SkillFormat> skills = new ArrayList<>();

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
                        }
                        continue;
                    }
                    // We are now inside the actual skill body
                    body.append(line).append("\n");
                }
                skill.setBody(body.toString());
                skills.add(skill);
                skill_prompt.put(
                        "/"+skill.getName(),
                        skill.getBody()
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

        for (SkillFormat skill : skill_list) {
            prompt.append(skill).append("\n");
        }

        return prompt.toString();
    }

    public String getPrompt() {
        return prompt;
    }
    public String getSkillPrompt(String command){
        // get the prompt of the command from the hashmap
        return skill_prompt.get(command);
    }
    public String subsituteInPrompt(String oldPrompt){
        String newPrompt = oldPrompt;
        // look for commands ;
        String[] cmds = Arrays.stream(oldPrompt.split("\\s+"))
                .filter(word -> word.startsWith("/"))
                .toArray(String[]::new);

        for (String cmd : cmds) {
            String skillPrompt = this.getSkillPrompt(cmd);

            if (skillPrompt != null) {
                // check for Arguments
                Argument arg = new Argument(cmd , skillPrompt , oldPrompt);

                // substitue allArgs ;
                newPrompt = arg.substituteArgs(skillPrompt);

                // replace cmd with the new body
                newPrompt = newPrompt.replace(cmd, skillPrompt);
            }
        }
        return newPrompt;
    }
}
