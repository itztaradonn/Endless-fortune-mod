package com.endless.fortune.data;

import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID playerUuid;
    private double luck;
    private String skillId;
    private final Set<String> completedAdvancements;
    private final Set<String> unlockedAbilities;

    public PlayerData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.luck = 0.0;
        this.skillId = null;
        this.completedAdvancements = new HashSet<>();
        this.unlockedAbilities = new HashSet<>();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public double getLuck() {
        return luck;
    }

    public void setLuck(double luck) {
        this.luck = Math.max(0.0, Math.min(100.0, luck));
    }

    public void addLuck(double amount) {
        setLuck(this.luck + amount);
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
        this.unlockedAbilities.clear();
    }

    public Skill getSkill() {
        if (skillId == null) return null;
        return SkillManager.getSkill(skillId);
    }

    public Set<String> getCompletedAdvancements() {
        return completedAdvancements;
    }

    public boolean hasCompletedAdvancement(String advancementId) {
        return completedAdvancements.contains(advancementId);
    }

    public void addCompletedAdvancement(String advancementId) {
        completedAdvancements.add(advancementId);
    }

    public Set<String> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    public boolean hasUnlockedAbility(String abilityId) {
        return unlockedAbilities.contains(abilityId);
    }

    public void unlockAbility(String abilityId) {
        unlockedAbilities.add(abilityId);
    }

    public void checkAndUnlockAbilities() {
        Skill skill = getSkill();
        if (skill == null) return;
        skill.getAbilities().forEach(ability -> {
            if (luck >= ability.requiredLuck()) {
                unlockedAbilities.add(ability.id());
            } else {
                unlockedAbilities.remove(ability.id());
            }
        });
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("uuid", playerUuid.toString());
        nbt.putDouble("luck", luck);
        if (skillId != null) {
            nbt.putString("skillId", skillId);
        }

        NbtList advList = new NbtList();
        for (String adv : completedAdvancements) {
            advList.add(NbtString.of(adv));
        }
        nbt.put("completedAdvancements", advList);

        NbtList abilityList = new NbtList();
        for (String ability : unlockedAbilities) {
            abilityList.add(NbtString.of(ability));
        }
        nbt.put("unlockedAbilities", abilityList);

        return nbt;
    }

    public static PlayerData fromNbt(NbtCompound nbt) {
        UUID uuid = UUID.fromString(nbt.getString("uuid", ""));
        PlayerData data = new PlayerData(uuid);
        data.luck = nbt.getDouble("luck", 0.0);
        String skillIdValue = nbt.getString("skillId", "");
        if (!skillIdValue.isEmpty()) {
            data.skillId = skillIdValue;
        }

        NbtList advList = nbt.getListOrEmpty("completedAdvancements");
        for (int i = 0; i < advList.size(); i++) {
            data.completedAdvancements.add(advList.getString(i, ""));
        }

        NbtList abilityList = nbt.getListOrEmpty("unlockedAbilities");
        for (int i = 0; i < abilityList.size(); i++) {
            data.unlockedAbilities.add(abilityList.getString(i, ""));
        }

        return data;
    }
}
