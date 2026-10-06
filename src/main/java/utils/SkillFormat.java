package utils;

public class SkillFormat {
    private String name;
    private String description;
    private String context = null;
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

    public String getContext() {
        return context;
    }
    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "- "+this.name+": "+this.description;
    }
}
