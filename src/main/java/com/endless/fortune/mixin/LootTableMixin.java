package com.endless.fortune.mixin;

import com.endless.fortune.luck.LootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LootTable.class)
public class LootTableMixin {

    @Inject(method = "generateLoot(Lnet/minecraft/loot/context/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"), cancellable = true)
    private void endlessfortune$modifyGeneratedLoot(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        Entity entity = null;
        try {
            entity = context.get(LootContextParameters.THIS_ENTITY);
        } catch (Exception ignored) {
            // LootContextParameters.THIS_ENTITY may not be present in all contexts
        }

        if (entity instanceof ServerPlayerEntity player) {
            ObjectArrayList<ItemStack> originalLoot = cir.getReturnValue();
            if (originalLoot != null && !originalLoot.isEmpty()) {
                List<ItemStack> modifiedLoot = LootModifier.modifyLoot(player, originalLoot);
                cir.setReturnValue(new ObjectArrayList<>(modifiedLoot));
            }
        }
    }
}
