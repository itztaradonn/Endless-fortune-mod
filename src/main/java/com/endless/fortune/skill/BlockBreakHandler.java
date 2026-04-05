package com.endless.fortune.skill;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BlockBreakHandler {

    private static final Random RANDOM = new Random();

    // Prevent recursive block breaking
    private static boolean isBreaking = false;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            if (!(world instanceof ServerWorld serverWorld)) return;

            PlayerData data = PlayerDataManager.get(serverPlayer);
            if (data.getSkill() == null) return;

            // ═══════════════════════════════
            // MINER ABILITIES
            // ═══════════════════════════════
            if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
                handleMinerAbilities(serverPlayer, serverWorld, pos, state, data);
            }

            // ═══════════════════════════════
            // LUMBERJACK ABILITIES
            // ═══════════════════════════════
            if (state.isIn(BlockTags.LOGS)) {
                handleLumberjackAbilities(serverPlayer, serverWorld, pos, state, data);
            }
            if (state.isIn(BlockTags.LEAVES)) {
                handleLeafBreak(serverPlayer, serverWorld, pos, state, data);
            }

            // ═══════════════════════════════
            // FARMER ABILITIES
            // ═══════════════════════════════
            if (state.getBlock() instanceof CropBlock crop) {
                if (crop.isMature(state)) {
                    handleFarmerAbilities(serverPlayer, serverWorld, pos, state, data);
                }
            }
        });
    }

    // ═══════════════════════════════
    // MINER
    // ═══════════════════════════════
    private static void handleMinerAbilities(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, PlayerData data) {
        // Vein Riches: 15% chance to double ore drops
        if (data.hasUnlockedAbility("miner_double_ore")) {
            if (isOre(state) && RANDOM.nextFloat() < 0.15f) {
                Block.dropStacks(state, world, pos, null, player, player.getMainHandStack());
                player.sendMessage(Text.literal("  ✧ Double ore!").formatted(Formatting.AQUA, Formatting.ITALIC), true);
            }
        }

        // Gem Finder: Small chance to find gems when mining stone
        if (data.hasUnlockedAbility("miner_gem_finder")) {
            if (isStone(state) && RANDOM.nextFloat() < 0.02f) {
                ItemStack gem = getRandomGem();
                dropItem(world, pos, gem);
                player.sendMessage(Text.literal("  ✧ You found a gem!").formatted(Formatting.GREEN, Formatting.ITALIC), true);
                world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.5f);
            }
        }

        // Tunnel Bore: 5% chance to break adjacent blocks
        if (data.hasUnlockedAbility("miner_blast") && !isBreaking) {
            if (RANDOM.nextFloat() < 0.05f) {
                isBreaking = true;
                try {
                    for (BlockPos adjacent : getAdjacentPositions(pos)) {
                        BlockState adjState = world.getBlockState(adjacent);
                        if (adjState.isIn(BlockTags.PICKAXE_MINEABLE) && !adjState.isAir()) {
                            world.breakBlock(adjacent, true, player);
                        }
                    }
                    player.sendMessage(Text.literal("  ✧ Tunnel Bore!").formatted(Formatting.YELLOW, Formatting.ITALIC), true);
                } finally {
                    isBreaking = false;
                }
            }
        }

        // Motherlode ultimate tracking
        if (data.hasUnlockedAbility("miner_motherlode")) {
            UUID uuid = player.getUuid();
            int count = SkillAbilityHandler.BLOCKS_MINED.getOrDefault(uuid, 0) + 1;
            SkillAbilityHandler.BLOCKS_MINED.put(uuid, count);
            if (count >= 64) {
                SkillAbilityHandler.onMinerUltimate(player);
                SkillAbilityHandler.BLOCKS_MINED.put(uuid, 0);
            }
        }

        // If Motherlode is active, double ore drops
        if (SkillAbilityHandler.isMotherlodeActive(player.getUuid())) {
            if (isOre(state)) {
                Block.dropStacks(state, world, pos, null, player, player.getMainHandStack());
            }
        }
    }

    // ═══════════════════════════════
    // LUMBERJACK
    // ═══════════════════════════════
    private static void handleLumberjackAbilities(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, PlayerData data) {
        // Timber!: 20% chance to double log drops
        if (data.hasUnlockedAbility("lumber_double_wood")) {
            if (RANDOM.nextFloat() < 0.20f) {
                Block.dropStacks(state, world, pos, null, player, player.getMainHandStack());
                player.sendMessage(Text.literal("  ✧ Double logs!").formatted(Formatting.GREEN, Formatting.ITALIC), true);
            }
        }

        // Strip Harvest: Breaking logs drops extra sticks
        if (data.hasUnlockedAbility("lumber_strip")) {
            int stickCount = 1 + RANDOM.nextInt(2);
            dropItem(world, pos, new ItemStack(Items.STICK, stickCount));
        }

        // Tree Feller: 10% chance to break all connected logs
        if (data.hasUnlockedAbility("lumber_fell") && !isBreaking) {
            if (RANDOM.nextFloat() < 0.10f) {
                isBreaking = true;
                try {
                    int broken = fellTree(world, pos, player, 32);
                    if (broken > 1) {
                        player.sendMessage(Text.literal("  ✧ Tree Feller! (" + broken + " logs)").formatted(Formatting.GREEN, Formatting.BOLD), true);
                        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 0.5f, 1.2f);
                    }
                } finally {
                    isBreaking = false;
                }
            }
        }

        // Timber Storm ultimate tracking
        if (data.hasUnlockedAbility("lumber_timberstorm")) {
            UUID uuid = player.getUuid();
            int count = SkillAbilityHandler.LOGS_CHOPPED.getOrDefault(uuid, 0) + 1;
            SkillAbilityHandler.LOGS_CHOPPED.put(uuid, count);
            if (count >= 32) {
                SkillAbilityHandler.onLumberjackUltimate(player);
                SkillAbilityHandler.LOGS_CHOPPED.put(uuid, 0);
            }
        }
    }

    private static void handleLeafBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, PlayerData data) {
        // Auto Replant: 30% chance to drop saplings from leaves
        if (data.hasUnlockedAbility("lumber_replant")) {
            if (RANDOM.nextFloat() < 0.30f) {
                dropItem(world, pos, new ItemStack(Items.OAK_SAPLING, 1));
            }
        }
    }

    // ═══════════════════════════════
    // FARMER
    // ═══════════════════════════════
    private static void handleFarmerAbilities(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, PlayerData data) {
        // Bountiful Harvest: 20% chance to double crop drops
        if (data.hasUnlockedAbility("farmer_double_harvest")) {
            if (RANDOM.nextFloat() < 0.20f) {
                Block.dropStacks(state, world, pos, null, player, player.getMainHandStack());
                player.sendMessage(Text.literal("  ✧ Bountiful Harvest!").formatted(Formatting.GREEN, Formatting.ITALIC), true);
            }
        }

        // Harvest Moon ultimate tracking
        if (data.hasUnlockedAbility("farmer_harvestmoon")) {
            UUID uuid = player.getUuid();
            int count = SkillAbilityHandler.CROPS_HARVESTED.getOrDefault(uuid, 0) + 1;
            SkillAbilityHandler.CROPS_HARVESTED.put(uuid, count);
            if (count >= 20) {
                SkillAbilityHandler.onFarmerUltimate(player);
                SkillAbilityHandler.CROPS_HARVESTED.put(uuid, 0);
            }
        }
    }

    // ═══════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════

    private static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.IRON_ORES) ||
                state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES) ||
                state.isIn(BlockTags.LAPIS_ORES) || state.isIn(BlockTags.REDSTONE_ORES) ||
                state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.COAL_ORES);
    }

    private static boolean isStone(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE ||
                block == Blocks.GRANITE || block == Blocks.DIORITE || block == Blocks.TUFF;
    }

    private static ItemStack getRandomGem() {
        return switch (RANDOM.nextInt(5)) {
            case 0 -> new ItemStack(Items.DIAMOND, 1);
            case 1 -> new ItemStack(Items.EMERALD, 1);
            case 2 -> new ItemStack(Items.LAPIS_LAZULI, RANDOM.nextInt(3) + 1);
            case 3 -> new ItemStack(Items.AMETHYST_SHARD, RANDOM.nextInt(2) + 1);
            default -> new ItemStack(Items.QUARTZ, RANDOM.nextInt(2) + 1);
        };
    }

    private static void dropItem(ServerWorld world, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        world.spawnEntity(entity);
    }

    private static List<BlockPos> getAdjacentPositions(BlockPos pos) {
        return List.of(
                pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()
        );
    }

    private static int fellTree(ServerWorld world, BlockPos start, ServerPlayerEntity player, int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();
        toVisit.add(start);
        int broken = 0;

        while (!toVisit.isEmpty() && broken < maxBlocks) {
            BlockPos current = toVisit.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockState state = world.getBlockState(current);
            if (!state.isIn(BlockTags.LOGS)) continue;

            if (!current.equals(start)) {
                world.breakBlock(current, true, player);
                broken++;
            }

            // Check all adjacent blocks and blocks above
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockPos next = current.add(dx, dy, dz);
                        if (!visited.contains(next)) {
                            toVisit.add(next);
                        }
                    }
                }
            }
        }
        return broken;
    }
}
