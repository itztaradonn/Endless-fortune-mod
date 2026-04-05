package com.endless.fortune;

import com.endless.fortune.network.ActivateUltimatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class EndlessFortuneClient implements ClientModInitializer {

    private static KeyBinding activateUltimateKey;

    @Override
    public void onInitializeClient() {
        activateUltimateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.endlessfortune.activate_ultimate",
                GLFW.GLFW_KEY_R,
                KeyBinding.Category.GAMEPLAY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (activateUltimateKey.wasPressed()) {
                if (client.player != null && ClientPlayNetworking.canSend(ActivateUltimatePayload.ID)) {
                    ClientPlayNetworking.send(ActivateUltimatePayload.INSTANCE);
                }
            }
        });
    }
}
