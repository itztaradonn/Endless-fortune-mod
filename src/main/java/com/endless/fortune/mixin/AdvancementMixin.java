package com.endless.fortune.mixin;

import com.endless.fortune.luck.LuckManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class AdvancementMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void endlessfortune$onAdvancementGrant(AdvancementEntry advancement, String criterionName,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && owner != null) {
            PlayerAdvancementTracker tracker = (PlayerAdvancementTracker)(Object)this;
            if (advancement.id() != null) {
                var progress = tracker.getProgress(advancement);
                ServerWorld world = owner.getEntityWorld();
                boolean announcesInChat = advancement.value().display()
                        .map(display -> display.shouldAnnounceToChat())
                        .orElse(false)
                        && world.getGameRules().getValue(GameRules.ANNOUNCE_ADVANCEMENTS);
                if (progress.isDone() && announcesInChat) {
                    String advancementId = advancement.id().toString();
                    LuckManager.onAdvancementEarned(owner, advancementId);
                }
            }
        }
    }
}
