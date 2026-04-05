package com.endless.fortune.data;

import com.endless.fortune.EndlessFortune;
import com.endless.fortune.item.ModItems;
import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private static MinecraftServer server;

    public static void setServer(MinecraftServer srv) {
        server = srv;
        loadAll();
    }

    public static PlayerData getOrCreate(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public static PlayerData get(ServerPlayerEntity player) {
        return getOrCreate(player.getUuid());
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        PlayerData data = getOrCreate(player.getUuid());

        // Starter items (only if missing)
        if (!player.getInventory().contains(new ItemStack(ModItems.GUIDE_BOOK))) {
            player.getInventory().insertStack(new ItemStack(ModItems.GUIDE_BOOK));
        }

        if (data.getSkillId() == null) {
            Skill randomSkill = SkillManager.getRandomSkill();
            data.setSkillId(randomSkill.getId());
            player.sendMessage(Text.literal("Assigned Skill: ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(randomSkill.getName()).formatted(randomSkill.getCategory().getColor(), Formatting.BOLD))
                    .append(Text.literal(" (" + randomSkill.getCategory().getDisplayName() + ")").formatted(Formatting.GRAY)), true);

            save(player.getUuid());
        } else {
            data.checkAndUnlockAbilities();
            player.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Welcome back! Skill: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(data.getSkill().getName()).formatted(data.getSkill().getCategory().getColor(), Formatting.BOLD))
                    .append(Text.literal(" | Luck: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format("%.1f%%", data.getLuck())).formatted(Formatting.GREEN)));
        }
    }

    public static void save(UUID uuid) {
        if (server == null) return;
        try {
            Path dir = getDataDir();
            File dirFile = dir.toFile();
            if (!dirFile.exists()) dirFile.mkdirs();
            Path file = dir.resolve(uuid.toString() + ".dat");
            PlayerData data = playerDataMap.get(uuid);
            if (data != null) {
                NbtIo.writeCompressed(data.toNbt(), file);
            }
        } catch (IOException e) {
            EndlessFortune.LOGGER.error("Failed to save player data for " + uuid, e);
        }
    }

    public static void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            save(uuid);
        }
    }

    public static void loadAll() {
        if (server == null) return;
        File dirFile = getDataDir().toFile();
        if (!dirFile.exists()) return;
        File[] files = dirFile.listFiles((d, name) -> name.endsWith(".dat"));
        if (files == null) return;
        for (File file : files) {
            try {
                NbtCompound nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                PlayerData data = PlayerData.fromNbt(nbt);
                playerDataMap.put(data.getPlayerUuid(), data);
            } catch (IOException e) {
                EndlessFortune.LOGGER.error("Failed to load player data from " + file.getName(), e);
            }
        }
    }

    private static Path getDataDir() {
        return server.getSavePath(WorldSavePath.ROOT).resolve("endlessfortune");
    }
}
