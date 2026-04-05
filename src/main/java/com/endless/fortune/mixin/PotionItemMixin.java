package com.endless.fortune.mixin;

import com.endless.fortune.skill.SkillAbilityHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class PotionItemMixin {

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void endlessfortune$onPotionConsumed(ItemStack stack, World world, LivingEntity user,
                                                 CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof PotionItem && !world.isClient() && user instanceof ServerPlayerEntity player) {
            SkillAbilityHandler.onPotionConsumed(player, stack);
        }
    }
}
