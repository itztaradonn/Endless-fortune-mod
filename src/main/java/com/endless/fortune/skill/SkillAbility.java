package com.endless.fortune.skill;

public record SkillAbility(
        String id,
        String name,
        String description,
        double requiredLuck
) {
    @Override
    public String toString() {
        return String.format("%s (%.1f%% Luck) - %s", name, requiredLuck, description);
    }
}
