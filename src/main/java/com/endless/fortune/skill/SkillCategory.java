package com.endless.fortune.skill;

import net.minecraft.util.Formatting;

public enum SkillCategory {
    COMBAT("Combat", Formatting.RED),
    UTILITY("Utility", Formatting.BLUE),
    GATHERING("Gathering", Formatting.GREEN);

    private final String displayName;
    private final Formatting color;

    SkillCategory(String displayName, Formatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Formatting getColor() {
        return color;
    }
}
