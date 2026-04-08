package com.endless.fortune.luck;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class LootModifier {

    private static final Random RANDOM = new Random();

    // Upgrade map: from item -> to item
    private static final Map<Item, Item> UPGRADE_MAP = new LinkedHashMap<>();

    static {
        // Weapons
        UPGRADE_MAP.put(Items.WOODEN_SWORD, Items.STONE_SWORD);
        UPGRADE_MAP.put(Items.STONE_SWORD, Items.IRON_SWORD);
        UPGRADE_MAP.put(Items.IRON_SWORD, Items.DIAMOND_SWORD);

        // Pickaxes
        UPGRADE_MAP.put(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE);
        UPGRADE_MAP.put(Items.STONE_PICKAXE, Items.IRON_PICKAXE);
        UPGRADE_MAP.put(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE);

        // Axes
        UPGRADE_MAP.put(Items.WOODEN_AXE, Items.STONE_AXE);
        UPGRADE_MAP.put(Items.STONE_AXE, Items.IRON_AXE);
        UPGRADE_MAP.put(Items.IRON_AXE, Items.DIAMOND_AXE);

        // Shovels
        UPGRADE_MAP.put(Items.WOODEN_SHOVEL, Items.STONE_SHOVEL);
        UPGRADE_MAP.put(Items.STONE_SHOVEL, Items.IRON_SHOVEL);
        UPGRADE_MAP.put(Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL);

        // Armor - Helmet
        UPGRADE_MAP.put(Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET);
        UPGRADE_MAP.put(Items.CHAINMAIL_HELMET, Items.IRON_HELMET);
        UPGRADE_MAP.put(Items.IRON_HELMET, Items.DIAMOND_HELMET);

        // Armor - Chestplate
        UPGRADE_MAP.put(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
        UPGRADE_MAP.put(Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE);
        UPGRADE_MAP.put(Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE);

        // Armor - Leggings
        UPGRADE_MAP.put(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
        UPGRADE_MAP.put(Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS);
        UPGRADE_MAP.put(Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS);

        // Armor - Boots
        UPGRADE_MAP.put(Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS);
        UPGRADE_MAP.put(Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS);
        UPGRADE_MAP.put(Items.IRON_BOOTS, Items.DIAMOND_BOOTS);

        // Materials
        UPGRADE_MAP.put(Items.COAL, Items.IRON_INGOT);
        UPGRADE_MAP.put(Items.IRON_INGOT, Items.GOLD_INGOT);
        UPGRADE_MAP.put(Items.GOLD_INGOT, Items.DIAMOND);
        UPGRADE_MAP.put(Items.EMERALD, Items.DIAMOND);

        // Food
        UPGRADE_MAP.put(Items.BREAD, Items.COOKED_BEEF);
        UPGRADE_MAP.put(Items.COOKED_BEEF, Items.GOLDEN_APPLE);
        UPGRADE_MAP.put(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
    }

    /**
     * Modifies loot items based on a player's luck stat.
     * Returns the modified list of items.
     */
    public static List<ItemStack> modifyLoot(ServerPlayerEntity player, List<ItemStack> originalLoot) {
        PlayerData data = PlayerDataManager.get(player);
        double luck = data.getLuck();

        if (luck <= 10.0) {
            return originalLoot; // Low luck = normal loot
        }

        List<ItemStack> modifiedLoot = new ArrayList<>();
        boolean wasUpgraded = false;

        double upgradeChance = luck / 200.0; // At 100% luck, 50% chance per item

        for (ItemStack stack : originalLoot) {
            if (RANDOM.nextDouble() < upgradeChance) {
                ItemStack upgraded = tryUpgradeItem(stack);
                if (upgraded != stack) {
                    modifiedLoot.add(upgraded);
                    wasUpgraded = true;
                    continue;
                }
            }
            modifiedLoot.add(stack.copy());
        }

        // If any item was upgraded, play the amethyst chime
        if (wasUpgraded) {
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("  ✧ ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Your luck influenced this loot!").formatted(Formatting.LIGHT_PURPLE,
                            Formatting.ITALIC)));
        }

        return modifiedLoot;
    }

    private static ItemStack tryUpgradeItem(ItemStack original) {
        Item upgraded = UPGRADE_MAP.get(original.getItem());
        if (upgraded != null) {
            ItemStack newStack = new ItemStack(upgraded, original.getCount());
            // Copy enchantments if any using 1.21.1 component system
            ItemEnchantmentsComponent enchantments = original.getEnchantments();
            if (!enchantments.isEmpty()) {
                newStack.set(DataComponentTypes.ENCHANTMENTS, enchantments);
            }
            // Copy custom name if any
            if (original.contains(DataComponentTypes.CUSTOM_NAME)) {
                newStack.set(DataComponentTypes.CUSTOM_NAME, original.getName());
            }
            return newStack;
        }

        // Try to upgrade enchantment levels on enchanted items/books
        ItemEnchantmentsComponent enchantments = original.getItem() == Items.ENCHANTED_BOOK
                ? original.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                : original.getEnchantments();

        if (!enchantments.isEmpty()) {
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                    ItemEnchantmentsComponent.DEFAULT);
            boolean changed = false;

            for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
                int level = enchantments.getLevel(entry);
                int maxLevel = entry.value().getMaxLevel();
                if (level < maxLevel) {
                    builder.add(entry, level + 1);
                    changed = true;
                } else {
                    builder.add(entry, level);
                }
            }

            if (changed) {
                ItemStack copy = original.copy();
                if (original.getItem() == Items.ENCHANTED_BOOK) {
                    copy.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
                } else {
                    copy.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                }
                return copy;
            }
        }

        return original; // No upgrade available
    }
}
