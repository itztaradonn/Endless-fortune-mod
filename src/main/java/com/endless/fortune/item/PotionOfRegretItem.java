package com.endless.fortune.item;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.skill.Skill;
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

public class PotionOfRegretItem extends Item {

    public PotionOfRegretItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            PlayerData data = PlayerDataManager.get(player);
            String oldSkillId = data.getSkillId();
            Skill oldSkill = data.getSkill();

            // Get a new random skill (different from current)
            Skill newSkill;
            int attempts = 0;
            do {
                newSkill = SkillManager.getRandomSkill();
                attempts++;
            } while (newSkill.getId().equals(oldSkillId) && attempts < 20);

            data.setSkillId(newSkill.getId());
            data.checkAndUnlockAbilities();

            // Consume the item
            stack.decrement(1);

            // Visual + Sound feedback
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 1.0f, 0.5f);
            player.getEntityWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(Text.literal("Rerolled skill → ").formatted(Formatting.DARK_PURPLE)
                    .append(Text.literal(newSkill.getName()).formatted(newSkill.getCategory().getColor(), Formatting.BOLD)), true);

            PlayerDataManager.save(player.getUuid());

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.literal("A bitter potion brewed from regret.").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Right-click to reroll your skill.").formatted(Formatting.YELLOW));
        tooltip.accept(Text.literal("Your abilities will be reset!").formatted(Formatting.RED));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
