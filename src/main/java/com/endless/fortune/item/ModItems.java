package com.endless.fortune.item;

import com.endless.fortune.EndlessFortune;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {

    // Registry keys for items (required since 1.21.2)
    private static final RegistryKey<Item> POTION_OF_REGRET_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "potion_of_regret"));
    private static final RegistryKey<Item> COMBAT_SKILL_TOME_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "combat_skill_tome"));
    private static final RegistryKey<Item> UTILITY_SKILL_TOME_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "utility_skill_tome"));
    private static final RegistryKey<Item> GATHERING_SKILL_TOME_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "gathering_skill_tome"));
    private static final RegistryKey<Item> LUCK_CRYSTAL_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "luck_crystal"));
    private static final RegistryKey<Item> POTION_OF_FORTUNE_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "potion_of_fortune"));
    private static final RegistryKey<Item> GUIDE_BOOK_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "guide_book"));
    private static final RegistryKey<Item> SKILL_BUTTON_KEY = RegistryKey.of(
            RegistryKeys.ITEM, Identifier.of(EndlessFortune.MOD_ID, "skill_button"));

    // Potion of Regret - rerolls skill
    public static final Item POTION_OF_REGRET = new PotionOfRegretItem(
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC).registryKey(POTION_OF_REGRET_KEY));

    // Skill Tomes - craftable skill items that reset luck
    public static final Item COMBAT_SKILL_TOME = new SkillTomeItem(
            new Item.Settings().maxCount(1).rarity(Rarity.RARE).registryKey(COMBAT_SKILL_TOME_KEY), SkillTomeItem.TomeType.COMBAT);

    public static final Item UTILITY_SKILL_TOME = new SkillTomeItem(
            new Item.Settings().maxCount(1).rarity(Rarity.RARE).registryKey(UTILITY_SKILL_TOME_KEY), SkillTomeItem.TomeType.UTILITY);

    public static final Item GATHERING_SKILL_TOME = new SkillTomeItem(
            new Item.Settings().maxCount(1).rarity(Rarity.RARE).registryKey(GATHERING_SKILL_TOME_KEY), SkillTomeItem.TomeType.GATHERING);

    // Luck Crystal - rare crafting ingredient
    public static final Item LUCK_CRYSTAL = new Item(
            new Item.Settings().rarity(Rarity.UNCOMMON).registryKey(LUCK_CRYSTAL_KEY));

    // Potion of Fortune - restores 20% luck when consumed
    public static final Item POTION_OF_FORTUNE = new PotionOfFortuneItem(
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC).registryKey(POTION_OF_FORTUNE_KEY), 20.0);

    // Starter items
    public static final Item GUIDE_BOOK = new GuideBookItem(
            new Item.Settings().maxCount(1).rarity(Rarity.RARE).registryKey(GUIDE_BOOK_KEY));
    public static final Item SKILL_BUTTON = new SkillButtonItem(
            new Item.Settings().maxCount(1).rarity(Rarity.RARE).registryKey(SKILL_BUTTON_KEY));

    // Item Group
    public static final RegistryKey<ItemGroup> ENDLESS_FORTUNE_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(EndlessFortune.MOD_ID, "endless_fortune"));

    public static void register() {
        // Register items using RegistryKey (required since 1.21.2)
        Registry.register(Registries.ITEM, POTION_OF_REGRET_KEY, POTION_OF_REGRET);
        Registry.register(Registries.ITEM, COMBAT_SKILL_TOME_KEY, COMBAT_SKILL_TOME);
        Registry.register(Registries.ITEM, UTILITY_SKILL_TOME_KEY, UTILITY_SKILL_TOME);
        Registry.register(Registries.ITEM, GATHERING_SKILL_TOME_KEY, GATHERING_SKILL_TOME);
        Registry.register(Registries.ITEM, LUCK_CRYSTAL_KEY, LUCK_CRYSTAL);
        Registry.register(Registries.ITEM, POTION_OF_FORTUNE_KEY, POTION_OF_FORTUNE);
        Registry.register(Registries.ITEM, GUIDE_BOOK_KEY, GUIDE_BOOK);
        Registry.register(Registries.ITEM, SKILL_BUTTON_KEY, SKILL_BUTTON);

        // Register item group
        Registry.register(Registries.ITEM_GROUP, ENDLESS_FORTUNE_GROUP, FabricItemGroup.builder()
                .icon(() -> new ItemStack(POTION_OF_REGRET))
                .displayName(Text.literal("Endless Fortune").formatted(Formatting.GOLD))
                .entries((context, entries) -> {
                    entries.add(POTION_OF_REGRET);
                    entries.add(COMBAT_SKILL_TOME);
                    entries.add(UTILITY_SKILL_TOME);
                    entries.add(GATHERING_SKILL_TOME);
                    entries.add(LUCK_CRYSTAL);
                    entries.add(POTION_OF_FORTUNE);
                    entries.add(GUIDE_BOOK);
                })
                .build());

        EndlessFortune.LOGGER.info("Registered Endless Fortune items!");
    }
}
