package com.endless.fortune.luck;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


import java.util.HashMap;
import java.util.Map;

public class LuckManager {

    public enum AdvancementType {
        STANDARD(0.5, "Standard"),
        GOAL(2.0, "Goal"),
        CHALLENGE(5.0, "Challenge");

        private final double luckBonus;
        private final String displayName;

        AdvancementType(double luckBonus, String displayName) {
            this.luckBonus = luckBonus;
            this.displayName = displayName;
        }

        public double getLuckBonus() {
            return luckBonus;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Map advancement IDs to their types
    private static final Map<String, AdvancementType> ADVANCEMENT_TYPES = new HashMap<>();

    static {
        // ═══════════════════════════════
        // CHALLENGE ADVANCEMENTS (+5.0%)
        // ═══════════════════════════════
        ADVANCEMENT_TYPES.put("minecraft:nether/uneasy_alliance", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:nether/all_potions", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:nether/all_effects", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:end/dragon_breath", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:end/respawn_dragon", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:end/elytra", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:adventure/hero_of_the_village", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:adventure/two_birds_one_arrow", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:adventure/arbalistic", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:adventure/adventuring_time", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/balanced_diet", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/complete_catalogue", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/bred_all_animals", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:nether/explore_nether", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:adventure/kill_all_mobs", AdvancementType.CHALLENGE);
        ADVANCEMENT_TYPES.put("minecraft:nether/create_full_beacon", AdvancementType.CHALLENGE);

        // ═══════════════════════════════
        // GOAL ADVANCEMENTS (+2.0%)
        // ═══════════════════════════════
        ADVANCEMENT_TYPES.put("minecraft:story/obtain_armor", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:story/lava_bucket", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:story/enchant_item", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:story/cure_zombie_villager", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:story/follow_ender_eye", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:story/enter_the_end", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/find_fortress", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/obtain_blaze_rod", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/get_wither_skull", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/summon_wither", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/brew_potion", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:nether/create_beacon", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:end/kill_dragon", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:end/enter_end_gateway", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:end/find_end_city", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:adventure/totem_of_undying", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:adventure/summon_iron_golem", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:adventure/trade", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/tame_an_animal", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/make_a_sign_glow", AdvancementType.GOAL);
        ADVANCEMENT_TYPES.put("minecraft:husbandry/obtain_netherite_hoe", AdvancementType.GOAL);

        // All other advancements default to STANDARD (+0.5%)
    }

    public static AdvancementType getAdvancementType(String advancementId) {
        return ADVANCEMENT_TYPES.getOrDefault(advancementId, AdvancementType.STANDARD);
    }

    public static void onAdvancementEarned(ServerPlayerEntity player, String advancementId) {
        PlayerData data = PlayerDataManager.get(player);

        if (data.hasCompletedAdvancement(advancementId)) {
            return; // Already processed
        }

        AdvancementType type = getAdvancementType(advancementId);
        double bonus = type.getLuckBonus();
        double oldLuck = data.getLuck();

        data.addCompletedAdvancement(advancementId);
        data.addLuck(bonus);

        // Check for newly unlocked abilities
        data.checkAndUnlockAbilities();

        // Notify player
        Formatting bonusColor = switch (type) {
            case CHALLENGE -> Formatting.LIGHT_PURPLE;
            case GOAL -> Formatting.GOLD;
            case STANDARD -> Formatting.GREEN;
        };

        player.sendMessage(Text.literal("  ★ ").formatted(Formatting.GOLD)
                .append(Text.literal(String.format("+%.1f%% Luck", bonus)).formatted(bonusColor, Formatting.BOLD))
                .append(Text.literal(String.format(" (%.1f%% → %.1f%%)", oldLuck, data.getLuck())).formatted(Formatting.GRAY))
                .append(Text.literal(" [" + type.getDisplayName() + "]").formatted(bonusColor)));

        // Play sound based on advancement type
        switch (type) {
            case CHALLENGE -> player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            case GOAL -> player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
            case STANDARD -> player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Check for newly unlocked abilities and notify
        if (data.getSkill() != null) {
            data.getSkill().getAbilities().forEach(ability -> {
                if (data.hasUnlockedAbility(ability.id()) && oldLuck < ability.requiredLuck() && data.getLuck() >= ability.requiredLuck()) {
                    player.sendMessage(Text.literal("  ✦ ABILITY UNLOCKED: ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(ability.name()).formatted(Formatting.WHITE, Formatting.BOLD))
                            .append(Text.literal(" - " + ability.description()).formatted(Formatting.GRAY)));
                    player.getEntityWorld().playSound(null, player.getBlockPos(),
                            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            });
        }

        PlayerDataManager.save(player.getUuid());
    }
}
