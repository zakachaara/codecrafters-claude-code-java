package utils;

import utils.tools.SkillFormat;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    private List<SkillFormat> readSkills(List<String> paths){

        try {
            ArrayList<SkillFormat> skills = new ArrayList<>();

            for (String filePath : paths) {
                Path path = Paths.get(filePath);
                SkillFormat skill = new SkillFormat();
                try (BufferedReader br = Files.newBufferedReader(path)) {
                    int countOfMarker = 0;
                    String line;
                    // Build the body ;
                    StringBuilder body = new StringBuilder();

                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("---")) {
                            countOfMarker++;
                            continue;
                        }
                        if (line.startsWith("name:")) {
                            skill.setName(line.substring("name:".length()).trim());
                            continue;
                        }
                        if (line.startsWith("description:")) {
                            skill.setDescription(
                                    line.substring("description:".length()).trim()
                            );
                        }
                        else {
                            body.append(line);
                        }
                    }
                    skill.setBody(body.toString());
                    // add the skill to the list of skills
                    skills.add(skill);
                    // use the map to store each skill with its body
                    skill_prompt.put(skill.getName(), skill.getBody());
                }
            }
            return skills;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return skill_prompt.get(command);
    }
}
