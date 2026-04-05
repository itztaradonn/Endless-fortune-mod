package com.endless.fortune.skill;

import java.util.List;

public class Skill {

    private final String id;
    private final String name;
    private final String description;
    private final SkillCategory category;
    private final List<SkillAbility> abilities;

    public Skill(String id, String name, String description, SkillCategory category, List<SkillAbility> abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.abilities = abilities;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public List<SkillAbility> getAbilities() {
        return abilities;
    }
}
