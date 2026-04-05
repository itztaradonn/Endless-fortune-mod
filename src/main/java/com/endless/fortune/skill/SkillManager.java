package com.endless.fortune.skill;

import java.util.*;

public class SkillManager {

    private static final Map<String, Skill> SKILLS = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    public static void init() {
        // ═══════════════════════════════
        // COMBAT SKILLS
        // ═══════════════════════════════

        registerSkill(new Skill("berserker", "Berserker", "Channel your rage to deal devastating damage.",
                SkillCategory.COMBAT, List.of(
                new SkillAbility("berserker_rage", "Rage", "Deal +20% melee damage when below 50% HP.", 5.0),
                new SkillAbility("berserker_bloodlust", "Bloodlust", "Heal 1 heart on each kill.", 15.0),
                new SkillAbility("berserker_frenzy", "Frenzy", "Gain Speed I for 5s after a kill.", 30.0),
                new SkillAbility("berserker_unstoppable", "Unstoppable", "Gain Resistance I when below 30% HP.", 50.0),
                new SkillAbility("berserker_warcry", "War Cry", "Deal +40% melee damage when below 25% HP.", 75.0),
                new SkillAbility("berserker_bloodbath", "Bloodbath", "Kill 5 mobs within 30s to unleash Strength III + Speed II + Fire Resistance for 15s.", 100.0)
        )));

        registerSkill(new Skill("guardian", "Guardian", "A defensive warrior who absorbs damage.",
                SkillCategory.COMBAT, List.of(
                new SkillAbility("guardian_shield", "Iron Skin", "Take 10% less damage from all sources.", 5.0),
                new SkillAbility("guardian_thorns", "Thorns Aura", "Attackers take 1 heart of damage.", 15.0),
                new SkillAbility("guardian_fortify", "Fortify", "Gain Resistance I while sneaking.", 30.0),
                new SkillAbility("guardian_aegis", "Aegis", "20% chance to completely negate damage.", 50.0),
                new SkillAbility("guardian_immortal", "Undying", "Once per 10 minutes, survive a fatal blow with 1 HP.", 75.0),
                new SkillAbility("guardian_fortress", "Fortress", "Survive 10 hits within 60s to gain Resistance III + Absorption V + Regen II for 20s.", 100.0)
        )));

        registerSkill(new Skill("archer", "Sharpshooter", "Master of ranged combat and precision.",
                SkillCategory.COMBAT, List.of(
                new SkillAbility("archer_steady", "Steady Aim", "Arrows deal +15% damage.", 5.0),
                new SkillAbility("archer_quickdraw", "Quick Draw", "Partially-charged arrows deal bonus damage instead of reduced.", 15.0),
                new SkillAbility("archer_pierce", "Piercing Shot", "Arrows have a 15% chance to deal +30% armor-piercing damage.", 30.0),
                new SkillAbility("archer_multishot", "Multi Shot", "10% chance to deal double arrow damage.", 50.0),
                new SkillAbility("archer_sniper", "Sniper", "Arrows deal +50% damage at long range (>20 blocks).", 75.0),
                new SkillAbility("archer_deadeye", "Dead Eye", "Hit 7 consecutive arrow shots to gain Strength III + Speed II for 10s.", 100.0)
        )));

        registerSkill(new Skill("necromancer", "Necromancer", "Command the undead and drain life force.",
                SkillCategory.COMBAT, List.of(
                new SkillAbility("necro_drain", "Life Drain", "Heal 0.5 hearts on each melee hit.", 5.0),
                new SkillAbility("necro_undead_friend", "Undead Pact", "Take 50% less damage from undead mobs.", 15.0),
                new SkillAbility("necro_wither", "Wither Touch", "Melee attacks apply Wither I for 3s.", 30.0),
                new SkillAbility("necro_soul_harvest", "Soul Harvest", "Killing mobs gives bonus XP.", 50.0),
                new SkillAbility("necro_resurrection", "Dark Resurrection", "On fatal hit, survive with 1 HP and gain Wither Aura (5 min cooldown).", 75.0),
                new SkillAbility("necro_soulstorm", "Soul Storm", "Kill 10 mobs to unleash Wither II on all mobs within 8 blocks + Regen III for 10s.", 100.0)
        )));

        // ═══════════════════════════════
        // UTILITY SKILLS
        // ═══════════════════════════════

        registerSkill(new Skill("alchemist", "Alchemist", "Master of potions and brewing.",
                SkillCategory.UTILITY, List.of(
                new SkillAbility("alchemist_extend", "Extended Brew", "Positive status effects last 25% longer (tick-based refresh).", 5.0),
                new SkillAbility("alchemist_resist", "Poison Immunity", "Immune to Poison and Wither effects.", 15.0),
                new SkillAbility("alchemist_splash_range", "Wide Splash", "Gain Regeneration I for 5s when hit by any splash potion.", 30.0),
                new SkillAbility("alchemist_double_brew", "Double Brew", "25% chance to gain double duration when drinking potions.", 50.0),
                new SkillAbility("alchemist_philosopher", "Philosopher's Touch", "Positive effects from potions you drink gain +1 amplifier.", 75.0),
                new SkillAbility("alchemist_elixir", "Elixir Master", "Use 5 potions within 2 min to refresh all active effects with double duration for 30s.", 100.0)
        )));

        registerSkill(new Skill("explorer", "Explorer", "Born to wander and discover.",
                SkillCategory.UTILITY, List.of(
                new SkillAbility("explorer_speed", "Wanderlust", "Permanent Speed I effect.", 5.0),
                new SkillAbility("explorer_no_fall", "Feather Step", "Take 50% less fall damage.", 15.0),
                new SkillAbility("explorer_night_vision", "Night Eyes", "Gain Night Vision in dark areas.", 30.0),
                new SkillAbility("explorer_dolphin", "Aquatic Grace", "Gain Dolphin's Grace in water.", 50.0),
                new SkillAbility("explorer_no_hunger", "Endless Stamina", "Periodically gain Saturation to slow hunger.", 75.0),
                new SkillAbility("explorer_windwalker", "Wind Walker", "Sprint for 30 continuous seconds to gain Speed III + Jump Boost III + Slow Falling for 30s.", 100.0)
        )));

        registerSkill(new Skill("enchanter", "Enchanter", "Harness the arcane power of enchantments.",
                SkillCategory.UTILITY, List.of(
                new SkillAbility("enchanter_xp_boost", "Arcane Knowledge", "Gain bonus XP from killing mobs.", 5.0),
                new SkillAbility("enchanter_luck", "Enchanting Luck", "Gain Luck I effect permanently.", 15.0),
                new SkillAbility("enchanter_repair", "Mending Hands", "Slowly repair held items over time.", 30.0),
                new SkillAbility("enchanter_grindstone", "Salvage", "Gain 50% more XP from killing mobs.", 50.0),
                new SkillAbility("enchanter_mastery", "Arcane Mastery", "All held items repair twice as fast.", 75.0),
                new SkillAbility("enchanter_surge", "Arcane Surge", "Reach 30+ XP levels then sneak to consume 10 levels for Luck II + fast repair for 60s.", 100.0)
        )));

        registerSkill(new Skill("trickster", "Trickster", "A cunning rogue who bends the rules.",
                SkillCategory.UTILITY, List.of(
                new SkillAbility("trickster_stealth", "Shadow Step", "Become invisible for 3s when sneaking still (30s cooldown).", 5.0),
                new SkillAbility("trickster_pickpocket", "Sticky Fingers", "Mobs drop bonus XP and have 15% extra loot chance.", 15.0),
                new SkillAbility("trickster_dodge", "Evasion", "15% chance to dodge attacks.", 30.0),
                new SkillAbility("trickster_backstab", "Backstab", "Deal +50% damage when hitting from behind.", 50.0),
                new SkillAbility("trickster_escape", "Smoke Bomb", "When hit below 20% HP, gain Invisibility + Speed for 5s (5 min cooldown).", 75.0),
                new SkillAbility("trickster_shadowdance", "Shadow Dance", "Get 5 kills while invisible to gain 15s of unbreakable Invisibility + Speed II + Strength I.", 100.0)
        )));

        // ═══════════════════════════════
        // GATHERING SKILLS
        // ═══════════════════════════════

        registerSkill(new Skill("miner", "Miner", "The earth yields its treasures to you.",
                SkillCategory.GATHERING, List.of(
                new SkillAbility("miner_haste", "Quick Pickaxe", "Gain Haste I while holding a pickaxe.", 5.0),
                new SkillAbility("miner_double_ore", "Vein Riches", "15% chance to double ore drops.", 15.0),
                new SkillAbility("miner_gem_finder", "Gem Finder", "Small chance to find gems when mining stone.", 30.0),
                new SkillAbility("miner_sight", "Ore Sight", "Gain Night Vision while underground (y < 60).", 50.0),
                new SkillAbility("miner_blast", "Tunnel Bore", "Mining has a 5% chance to break adjacent blocks.", 75.0),
                new SkillAbility("miner_motherlode", "Motherlode", "Mine 64 blocks to activate Haste III + Night Vision for 30s.", 100.0)
        )));

        registerSkill(new Skill("farmer", "Farmer", "Nature blesses your harvests.",
                SkillCategory.GATHERING, List.of(
                new SkillAbility("farmer_growth", "Green Thumb", "Crops near you grow faster (periodic bone meal effect).", 5.0),
                new SkillAbility("farmer_double_harvest", "Bountiful Harvest", "20% chance to double crop drops.", 15.0),
                new SkillAbility("farmer_breeding", "Animal Whisperer", "Gain Regeneration I when near animals.", 30.0),
                new SkillAbility("farmer_nourish", "Nourishing Food", "Periodically gain Saturation when food bar is not full.", 50.0),
                new SkillAbility("farmer_nature", "Nature's Blessing", "Crops in a 5-block radius randomly grow each tick.", 75.0),
                new SkillAbility("farmer_harvestmoon", "Harvest Moon", "Harvest 20 crops to trigger: all crops in 8-block radius grow + Saturation + Hero of Village for 60s.", 100.0)
        )));

        registerSkill(new Skill("lumberjack", "Lumberjack", "Fell trees with incredible efficiency.",
                SkillCategory.GATHERING, List.of(
                new SkillAbility("lumber_chop", "Power Chop", "Gain Haste I while holding an axe.", 5.0),
                new SkillAbility("lumber_replant", "Auto Replant", "Breaking leaves has a 30% chance to drop saplings.", 15.0),
                new SkillAbility("lumber_double_wood", "Timber!", "20% chance to double log drops.", 30.0),
                new SkillAbility("lumber_strip", "Strip Harvest", "Breaking logs drops extra sticks.", 50.0),
                new SkillAbility("lumber_fell", "Tree Feller", "Breaking a log has 10% chance to break all connected logs.", 75.0),
                new SkillAbility("lumber_timberstorm", "Timber Storm", "Chop 32 logs to activate Haste III + Strength II for 30s.", 100.0)
        )));

        registerSkill(new Skill("fisherman", "Fisherman", "The waters reward your patience.",
                SkillCategory.GATHERING, List.of(
                new SkillAbility("fish_patience", "Patient Angler", "Gain Luck I while holding a fishing rod.", 5.0),
                new SkillAbility("fish_treasure", "Treasure Hunter", "Gain Luck II while holding a fishing rod in water.", 15.0),
                new SkillAbility("fish_double", "Double Catch", "Gain bonus XP when near water with a fishing rod.", 30.0),
                new SkillAbility("fish_rain_luck", "Rain Dance", "Gain Luck III while fishing in rain.", 50.0),
                new SkillAbility("fish_sea_blessing", "Sea's Blessing", "Gain Conduit Power while in water.", 75.0),
                new SkillAbility("fish_poseidon", "Poseidon's Blessing", "Stay in water for 60s to gain Luck III + Dolphin's Grace + Conduit Power for 60s.", 100.0)
        )));
    }

    private static void registerSkill(Skill skill) {
        SKILLS.put(skill.getId(), skill);
    }

    public static Skill getSkill(String id) {
        return SKILLS.get(id);
    }

    public static Collection<Skill> getAllSkills() {
        return SKILLS.values();
    }

    public static List<Skill> getSkillsByCategory(SkillCategory category) {
        return SKILLS.values().stream()
                .filter(s -> s.getCategory() == category)
                .toList();
    }

    public static Skill getRandomSkill() {
        List<Skill> all = new ArrayList<>(SKILLS.values());
        return all.get(RANDOM.nextInt(all.size()));
    }

    public static Skill getRandomSkillByCategory(SkillCategory category) {
        List<Skill> filtered = getSkillsByCategory(category);
        if (filtered.isEmpty()) return getRandomSkill();
        return filtered.get(RANDOM.nextInt(filtered.size()));
    }
}
