package com.endless.fortune.command;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WithdrawCommand {

    private static final double WITHDRAW_PERCENTAGE = 20.0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("withdraw")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayerOrThrow();
                    return executeWithdraw(player);
                })
        );
    }

    private static int executeWithdraw(ServerPlayerEntity player) {
        PlayerData data = PlayerDataManager.get(player);

        double currentLuck = data.getLuck();

        // Check if player has enough luck
        if (currentLuck < WITHDRAW_PERCENTAGE) {
            player.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal("You need at least ").formatted(Formatting.RED))
                    .append(Text.literal(String.format("%.0f%%", WITHDRAW_PERCENTAGE)).formatted(Formatting.YELLOW, Formatting.BOLD))
                    .append(Text.literal(" luck to withdraw!").formatted(Formatting.RED)));
            player.sendMessage(Text.literal("  Current luck: ").formatted(Formatting.GRAY)
                    .append(Text.literal(String.format("%.1f%%", currentLuck)).formatted(Formatting.RED)));
            return 0;
        }

        // Check inventory space
        if (player.getInventory().getEmptySlot() == -1) {
            player.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Your inventory is full!").formatted(Formatting.RED)));
            return 0;
        }

        // Reduce luck
        double newLuck = currentLuck - WITHDRAW_PERCENTAGE;
        data.setLuck(newLuck);
        data.checkAndUnlockAbilities(); // Re-check abilities (some may be locked now)
        PlayerDataManager.save(player.getUuid());

        // Give Potion of Fortune
        ItemStack potion = new ItemStack(ModItems.POTION_OF_FORTUNE);
        player.getInventory().insertStack(potion);

        // Effects
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 1.0f, 0.5f);
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.PLAYERS, 1.0f, 1.0f);

        player.sendMessage(
                Text.literal("Withdrawn ").formatted(Formatting.GOLD)
                        .append(Text.literal(String.format("%.0f%%", WITHDRAW_PERCENTAGE)).formatted(Formatting.RED, Formatting.BOLD))
                        .append(Text.literal(" luck → received Potion of Fortune").formatted(Formatting.GRAY)),
                true);

        return 1;
    }
}
