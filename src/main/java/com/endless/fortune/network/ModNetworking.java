package com.endless.fortune.network;

import com.endless.fortune.skill.SkillAbilityHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {

    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ActivateUltimatePayload.ID, ActivateUltimatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActivateUltimatePayload.ID,
                (payload, context) -> SkillAbilityHandler.tryActivateReadyUltimate(context.player()));
    }
}
