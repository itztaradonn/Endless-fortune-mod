package com.endless.fortune.item;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class PotionOfFortuneItem extends Item {

    private final double luckAmount;

    public PotionOfFortuneItem(Settings settings, double luckAmount) {
        super(settings);
        this.luckAmount = luckAmount;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity player) {
            if (!world.isClient()) {
                PlayerData data = PlayerDataManager.get(player);
                double oldLuck = data.getLuck();
                double newLuck = Math.min(100.0, oldLuck + luckAmount);
                data.setLuck(newLuck);

                // Check for newly unlocked abilities
                data.checkAndUnlockAbilities();
                PlayerDataManager.save(player.getUuid());

                // Effects and messages
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS, 1.0f, 1.2f);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS, 0.5f, 1.5f);

                player.sendMessage(Text.literal("Luck +").formatted(Formatting.GREEN)
                        .append(Text.literal(String.format("%.1f%%", (newLuck - oldLuck))).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" (Potion of Fortune)").formatted(Formatting.GRAY)), true);

                stack.decrement(1);
            }
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32; // Same as vanilla potions
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.literal("A potion brimming with fortune.").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Restores ").formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.0f%%", luckAmount)).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" Luck when consumed.").formatted(Formatting.GREEN)));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Obtained via /withdraw").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }

    public double getLuckAmount() {
        return luckAmount;
    }
}
