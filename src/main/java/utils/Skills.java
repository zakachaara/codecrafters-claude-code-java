package utils;

import utils.tools.SkillFormat;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Skills {
    List<SkillFormat> skill_list = new ArrayList<>();
    String prompt;
    public Skills() {
        // load .claude/skills and determine skill paths
        ArrayList<String> skillsPaths = loadSkillsPathFromFolder(Path.of(".claude/skills"));

        // loop over all the paths in it and read every Skill.md metadata
        this.skill_list = readSkills(skillsPaths);
        // Build the System prompt ;
        this.prompt = buildPrompt();
    }

    public List<SkillFormat> getSkill_list() {
        return skill_list;
    }

    private ArrayList<String> loadSkillsPathFromFolder(Path path){
        String[] files = path.toFile().list();

        ArrayList<String> SkillsPath_list = new ArrayList<>();

        for (String file : files) {
            // check if it is a folder
            if(Path.of(file).toFile().isDirectory()){
                file.concat("/SKILL.md");
                SkillsPath_list.add(file);
            }
        }
        return SkillsPath_list;
    }

    private ArrayList<SkillFormat> readSkills(ArrayList<String> paths){

        try {
            ArrayList<SkillFormat> skills = new ArrayList<>();

            for (String filePath : paths) {
                Path path = Paths.get(filePath);
                SkillFormat skill = new SkillFormat();
                try (BufferedReader br = Files.newBufferedReader(path)) {
                    int count_of_marker = 0 ;

                    for(String line : br.readLine().split("\n")){
                        if (line.startsWith("---")){
                            count_of_marker++;
                            continue;
                        }
                        if (count_of_marker == 2) break;
                        if (line.startsWith("name")){
                            // get the name from line and add it to the skill
                            skill.setName(line.replace("name: ", ""));
                            continue;
                        }
                        if (line.startsWith("description")){
                            skill.setDescription(line.replace("description: ", ""));
                        }
                    }
                    skills.add(skill);
                }
            }
            return skills;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(){
        String starter = "You have access to the following skills:\n\n";
        for (SkillFormat skill : skill_list) {
            starter = starter + skill.toString() + "\n";
        }
        return starter;
    }

    public String getPrompt() {
        return prompt;
    }
}
