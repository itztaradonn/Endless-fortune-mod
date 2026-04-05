package com.endless.fortune.item;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillCategory;
import com.endless.fortune.skill.SkillManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class SkillTomeItem extends Item {

    public enum TomeType {
        COMBAT(SkillCategory.COMBAT),
        UTILITY(SkillCategory.UTILITY),
        GATHERING(SkillCategory.GATHERING);

        private final SkillCategory category;

        TomeType(SkillCategory category) {
            this.category = category;
        }

        public SkillCategory getCategory() {
            return category;
        }
    }

    private static final double REQUIRED_LUCK_TO_USE = 100.0;

    private final TomeType tomeType;

    public SkillTomeItem(Settings settings, TomeType tomeType) {
        super(settings);
        this.tomeType = tomeType;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            PlayerData data = PlayerDataManager.get(player);
            if (data.getLuck() < REQUIRED_LUCK_TO_USE) {
                player.sendMessage(Text.literal("You need ").formatted(Formatting.RED)
                        .append(Text.literal(String.format("%.0f%%", REQUIRED_LUCK_TO_USE)).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" Luck to use this tome.").formatted(Formatting.RED)), true);
                return ActionResult.FAIL;
            }

            // Get a random skill from the specific category
            Skill newSkill = SkillManager.getRandomSkillByCategory(tomeType.getCategory());
            data.setSkillId(newSkill.getId());

            // RESET LUCK to 0
            data.setLuck(0.0);
            data.getCompletedAdvancements().clear();

            // Re-check abilities (should have none since luck is 0)
            data.checkAndUnlockAbilities();

            stack.decrement(1);

            // Sound
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.5f);
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 0.8f);
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1.5f);

            Formatting color = tomeType.getCategory().getColor();
            player.sendMessage(Text.literal("New Skill: ").formatted(Formatting.GOLD)
                    .append(Text.literal(newSkill.getName()).formatted(color, Formatting.BOLD))
                    .append(Text.literal(" (Luck reset to 0%)").formatted(Formatting.RED)), false);

            player.sendMessage(Text.literal("New Skill: ").formatted(Formatting.GOLD)
                    .append(Text.literal(newSkill.getName()).formatted(color, Formatting.BOLD))
                    .append(Text.literal(" (Luck reset to 0%)").formatted(Formatting.RED)), true);

            PlayerDataManager.save(player.getUuid());

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        Formatting color = tomeType.getCategory().getColor();
        tooltip.accept(Text.literal("A tome of " + tomeType.getCategory().getDisplayName() + " knowledge.").formatted(color, Formatting.ITALIC));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Requires 100% Luck to use.").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.accept(Text.literal("Right-click to apply.").formatted(Formatting.YELLOW));
        tooltip.accept(Text.literal("Grants a random " + tomeType.getCategory().getDisplayName() + " skill.").formatted(color));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("⚠ WARNING: Resets your Luck to 0%!").formatted(Formatting.RED, Formatting.BOLD));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
