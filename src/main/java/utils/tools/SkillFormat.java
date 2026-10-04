package utils.tools;

public class SkillFormat {
    private String name;
    private String description;
    public SkillFormat() {
//        this.name = name;
//        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "- "+this.name+": "+this.description;
    }
}
