package com.endless.fortune;

import com.endless.fortune.command.LuckCommand;
import com.endless.fortune.command.SkillCommand;
import com.endless.fortune.command.WithdrawCommand;
import com.endless.fortune.skill.BlockBreakHandler;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.item.ModItems;
import com.endless.fortune.network.ModNetworking;
import com.endless.fortune.skill.SkillManager;
import com.endless.fortune.skill.SkillAbilityHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndlessFortune implements ModInitializer {

    public static final String MOD_ID = "endlessfortune";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Endless Fortune initializing...");

        ModItems.register();
        SkillManager.init();
        ModNetworking.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LuckCommand.register(dispatcher);
            SkillCommand.register(dispatcher);
            WithdrawCommand.register(dispatcher);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerDataManager.onPlayerJoin(handler.getPlayer());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.setServer(server);
            LOGGER.info("Endless Fortune loaded successfully!");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataManager.saveAll();
        });

        SkillAbilityHandler.register();
        BlockBreakHandler.register();

        LOGGER.info("Endless Fortune initialized!");
    }
}
