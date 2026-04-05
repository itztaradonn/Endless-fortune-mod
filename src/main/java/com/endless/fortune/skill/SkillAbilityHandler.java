package com.endless.fortune.skill;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.item.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class SkillAbilityHandler {

    // Existing cooldowns
    private static final Map<UUID, Long> IMMORTAL_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> SMOKE_BOMB_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> STEALTH_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> SNEAK_TICK_COUNTER = new HashMap<>();
    private static final Map<UUID, Long> NECRO_RES_COOLDOWN = new HashMap<>();
    private static final Map<UUID, List<Long>> ALCHEMIST_POTION_TIMESTAMPS = new HashMap<>();

    // Ultimate tracking maps
    private static final Map<UUID, List<Long>> KILL_TIMESTAMPS = new HashMap<>();
    private static final Map<UUID, List<Long>> HIT_TAKEN_TIMESTAMPS = new HashMap<>();
    private static final Map<UUID, Integer> ARROW_HIT_STREAK = new HashMap<>();
    private static final Map<UUID, Long> LAST_ARROW_HIT_TIME = new HashMap<>();
    private static final Map<UUID, Integer> NECRO_KILL_COUNT = new HashMap<>();
    private static final Map<UUID, Integer> SPRINT_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> INVISIBLE_KILLS = new HashMap<>();
    private static final Map<UUID, Integer> WATER_TICKS = new HashMap<>();
    private static final Map<UUID, Long> ULTIMATE_COOLDOWN = new HashMap<>();
    private static final Map<UUID, String> ULTIMATE_READY = new HashMap<>();
    private static final Map<UUID, Long> LAST_ULTIMATE_COOLDOWN_ACTIONBAR = new HashMap<>();

    // Block break counters (used by BlockBreakHandler)
    public static final Map<UUID, Integer> BLOCKS_MINED = new HashMap<>();
    public static final Map<UUID, Integer> CROPS_HARVESTED = new HashMap<>();
    public static final Map<UUID, Integer> LOGS_CHOPPED = new HashMap<>();

    // Active ultimate buff tracking
    private static final Map<UUID, Long> DEAD_EYE_ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> MOTHERLODE_ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> ARCANE_SURGE_ACTIVE = new HashMap<>();
    private static final Set<UUID> APPLYING_MODIFIED_DAMAGE = new HashSet<>();

    private static final long ULTIMATE_GLOBAL_COOLDOWN = 2400L; // 2 min cooldown
    private static final double POTION_OF_FORTUNE_LUCK_COST = 20.0;

    public static void register() {
        // Tick-based passive abilities
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handlePassiveAbilities(player);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            UUID entityUuid = entity.getUuid();
            if (APPLYING_MODIFIED_DAMAGE.remove(entityUuid)) {
                return true;
            }

            float adjustedAmount = amount;

            ServerPlayerEntity attacker = resolveAttackingPlayer(source);
            if (attacker != null && isPlayerCombatHit(source, attacker)) {
                handlePlayerAttacking(attacker, entity, source);
                adjustedAmount = modifyOutgoingDamage(attacker, entity, source, adjustedAmount);
            }

            if (entity instanceof ServerPlayerEntity targetPlayer) {
                adjustedAmount = modifyIncomingDamage(targetPlayer, source, adjustedAmount);
                if (!handlePlayerDamaged(targetPlayer, source, adjustedAmount)) {
                    return false;
                }
                if (attacker != null) {
                    trackPlayerProjectileHit(attacker, targetPlayer, source);
                }
            }

            if (Math.abs(adjustedAmount - amount) > 0.001f) {
                if (adjustedAmount <= 0.0f) {
                    return false;
                }
                if (entity.getEntityWorld() instanceof ServerWorld world) {
                    APPLYING_MODIFIED_DAMAGE.add(entityUuid);
                    entity.damage(world, source, adjustedAmount);
                    return false;
                }
            }

            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            ServerPlayerEntity player = resolveAttackingPlayer(source);
            if (player != null) {
                handlePlayerKill(player, entity);
                if (entity instanceof ServerPlayerEntity deadPlayer && deadPlayer != player) {
                    dropPotionOfFortuneOnPvpDeath(deadPlayer);
                }
            }
            // Check if arrow from player killed the entity
            if (source.getSource() instanceof ProjectileEntity projectile) {
                if (projectile.getOwner() instanceof ServerPlayerEntity projectileOwner) {
                    handleProjectileKill(projectileOwner, entity);
                }
            }
        });
    }

    // ═══════════════════════════════════════
    // PASSIVE ABILITIES (tick-based)
    // ═══════════════════════════════════════
    private static void handlePassiveAbilities(ServerPlayerEntity player) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;
        expireTimedUltimateStates(player);
        showUltimateCooldownActionbar(player);

        // ── EXPLORER ──
        if (data.hasUnlockedAbility("explorer_speed")) {
            if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("explorer_night_vision")) {
            BlockPos pos = player.getBlockPos();
            if (player.getEntityWorld().getLightLevel(pos) < 8) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("explorer_dolphin")) {
            if (player.isTouchingWater()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("explorer_no_hunger")) {
            if (player.age % 100 == 0) { // Every 5 seconds
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 20, 0, true, false, false));
            }
        }
        // Explorer ultimate: Wind Walker - sprint tracking
        if (data.hasUnlockedAbility("explorer_windwalker")) {
            handleWindWalkerTracking(player);
        }

        // ── BERSERKER ──
        if (data.hasUnlockedAbility("berserker_unstoppable")) {
            if (player.getHealth() < player.getMaxHealth() * 0.3f) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0, true, false, true));
            }
        }

        // ── GUARDIAN ──
        if (data.hasUnlockedAbility("guardian_fortify")) {
            if (player.isSneaking()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0, true, false, true));
            }
        }

        // ── MINER ──
        if (data.hasUnlockedAbility("miner_haste")) {
            if (player.getMainHandStack().isIn(ItemTags.PICKAXES)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("miner_sight")) {
            if (player.getBlockPos().getY() < 60) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
        }

        // ── LUMBERJACK ──
        if (data.hasUnlockedAbility("lumber_chop")) {
            if (player.getMainHandStack().isIn(ItemTags.AXES)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 0, true, false, true));
            }
        }

        // ── TRICKSTER ──
        if (data.hasUnlockedAbility("trickster_stealth")) {
            handleStealthAbility(player);
        }

        // ── ALCHEMIST ──
        if (data.hasUnlockedAbility("alchemist_resist")) {
            if (player.hasStatusEffect(StatusEffects.POISON)) {
                player.removeStatusEffect(StatusEffects.POISON);
            }
            if (player.hasStatusEffect(StatusEffects.WITHER)) {
                player.removeStatusEffect(StatusEffects.WITHER);
            }
        }
        if (data.hasUnlockedAbility("alchemist_extend")) {
            // Refresh positive effects with extra duration every 60 ticks
            if (player.age % 60 == 0) {
                player.getActiveStatusEffects().forEach((effect, instance) -> {
                    if (instance.getDuration() > 0 && instance.getDuration() < 100 && !effect.value().isBeneficial()) {
                        // Skip non-beneficial, only extend beneficial
                    }
                    if (effect.value().isBeneficial() && instance.getDuration() > 20 && instance.getDuration() < 200) {
                        player.addStatusEffect(new StatusEffectInstance(effect, 
                                (int)(instance.getDuration() * 1.25f), instance.getAmplifier(), 
                                true, instance.shouldShowParticles(), instance.shouldShowIcon()));
                    }
                });
            }
        }

        // ── ENCHANTER ──
        if (data.hasUnlockedAbility("enchanter_luck")) {
            if (!player.hasStatusEffect(StatusEffects.LUCK)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("enchanter_repair")) {
            int interval = data.hasUnlockedAbility("enchanter_mastery") ? 100 : 200; // Mastery = 2x faster
            if (player.age % interval == 0) {
                ItemStack held = player.getMainHandStack();
                if (held.isDamaged()) {
                    held.setDamage(held.getDamage() - 1);
                }
                // Also repair offhand with mastery
                if (data.hasUnlockedAbility("enchanter_mastery")) {
                    ItemStack offhand = player.getOffHandStack();
                    if (offhand.isDamaged()) {
                        offhand.setDamage(offhand.getDamage() - 1);
                    }
                }
            }
        }
        // Enchanter ultimate: Arcane Surge - check activation
        if (data.hasUnlockedAbility("enchanter_surge")) {
            handleArcaneSurgeCheck(player);
        }
        // Active Arcane Surge - fast repair
        if (ARCANE_SURGE_ACTIVE.containsKey(player.getUuid())) {
            long expiry = ARCANE_SURGE_ACTIVE.get(player.getUuid());
            if (player.getEntityWorld().getTime() > expiry) {
                ARCANE_SURGE_ACTIVE.remove(player.getUuid());
            } else if (player.age % 40 == 0) {
                // Repair all inventory items
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (stack.isDamaged()) {
                        stack.setDamage(stack.getDamage() - 1);
                    }
                }
            }
        }

        // ── FARMER ──
        if (data.hasUnlockedAbility("farmer_growth")) {
            if (player.age % 100 == 0) { // Every 5 seconds, grow a nearby crop
                growNearbyCrops(player, 3);
            }
        }
        if (data.hasUnlockedAbility("farmer_nature")) {
            if (player.age % 40 == 0) { // More frequent crop growth
                growNearbyCrops(player, 5);
            }
        }
        if (data.hasUnlockedAbility("farmer_breeding")) {
            // Regeneration when near animals
            if (player.age % 60 == 0) {
                Box box = player.getBoundingBox().expand(8);
                List<AnimalEntity> animals = player.getEntityWorld().getEntitiesByClass(AnimalEntity.class, box, e -> true);
                if (!animals.isEmpty()) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, true, false, true));
                }
            }
        }
        if (data.hasUnlockedAbility("farmer_nourish")) {
            if (player.age % 80 == 0 && player.getHungerManager().getFoodLevel() < 20) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 20, 0, true, false, false));
            }
        }

        // ── FISHERMAN ──
        if (data.hasUnlockedAbility("fish_patience")) {
            if (player.getMainHandStack().isOf(Items.FISHING_ROD)) {
                int luckLevel = 0;
                if (data.hasUnlockedAbility("fish_rain_luck") && player.getEntityWorld().isRaining()) {
                    luckLevel = 2; // Luck III in rain
                } else if (data.hasUnlockedAbility("fish_treasure") && player.isTouchingWater()) {
                    luckLevel = 1; // Luck II in water
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 100, luckLevel, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("fish_sea_blessing")) {
            if (player.isTouchingWater()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 100, 0, true, false, true));
            }
        }
        if (data.hasUnlockedAbility("fish_double")) {
            if (player.age % 200 == 0 && player.getMainHandStack().isOf(Items.FISHING_ROD) && player.isTouchingWater()) {
                player.addExperience(3);
            }
        }
        // Fisherman ultimate: Poseidon's Blessing - water time tracking
        if (data.hasUnlockedAbility("fish_poseidon")) {
            handlePoseidonTracking(player);
        }
    }

    // ═══════════════════════════════════════
    // ULTIMATE TRACKING METHODS
    // ═══════════════════════════════════════

    private static void handleWindWalkerTracking(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (player.isSprinting()) {
            int ticks = SPRINT_TICKS.getOrDefault(uuid, 0) + 1;
            SPRINT_TICKS.put(uuid, ticks);
            if (ticks >= 600) { // 30 seconds of sprinting
                if (markUltimateReady(player, "Wind Walker")) {
                    SPRINT_TICKS.put(uuid, 0);
                }
            }
        } else {
            SPRINT_TICKS.put(uuid, 0);
        }
    }

    private static void handlePoseidonTracking(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (player.isTouchingWater()) {
            int ticks = WATER_TICKS.getOrDefault(uuid, 0) + 1;
            WATER_TICKS.put(uuid, ticks);
            if (ticks >= 1200) { // 60 seconds in water
                if (markUltimateReady(player, "Poseidon's Blessing")) {
                    WATER_TICKS.put(uuid, 0);
                }
            }
        } else {
            WATER_TICKS.put(uuid, 0);
        }
    }

    private static void handleArcaneSurgeCheck(ServerPlayerEntity player) {
        if (player.isSneaking() && player.experienceLevel >= 30) {
            markUltimateReady(player, "Arcane Surge");
        }
    }

    // Called when a projectile (arrow) deals damage to track archer streaks
    public static void onProjectileHit(ServerPlayerEntity player, Entity target) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;

        if (data.hasUnlockedAbility("archer_deadeye")) {
            UUID uuid = player.getUuid();
            long now = player.getEntityWorld().getTime();
            long lastHit = LAST_ARROW_HIT_TIME.getOrDefault(uuid, 0L);

            // Reset if more than 5 seconds between hits
            if (now - lastHit > 300) {
                ARROW_HIT_STREAK.put(uuid, 0);
            }

            int streak = ARROW_HIT_STREAK.getOrDefault(uuid, 0) + 1;
            ARROW_HIT_STREAK.put(uuid, streak);
            LAST_ARROW_HIT_TIME.put(uuid, now);

            if (streak >= 7) {
                if (markUltimateReady(player, "Dead Eye")) {
                    ARROW_HIT_STREAK.put(uuid, 0);
                }
            }
        }
    }

    // Called by BlockBreakHandler when miner mines 64 blocks
    public static void onMinerUltimate(ServerPlayerEntity player) {
        markUltimateReady(player, "Motherlode");
    }

    // Called by BlockBreakHandler when lumberjack chops 32 logs
    public static void onLumberjackUltimate(ServerPlayerEntity player) {
        markUltimateReady(player, "Timber Storm");
    }

    // Called by BlockBreakHandler when farmer harvests 20 crops
    public static void onFarmerUltimate(ServerPlayerEntity player) {
        markUltimateReady(player, "Harvest Moon");
    }

    public static boolean isMotherlodeActive(UUID uuid) {
        return MOTHERLODE_ACTIVE.containsKey(uuid) && MOTHERLODE_ACTIVE.get(uuid) > 0;
    }

    public static boolean isDeadEyeActive(UUID uuid) {
        return DEAD_EYE_ACTIVE.containsKey(uuid) && DEAD_EYE_ACTIVE.get(uuid) > 0;
    }

    // ═══════════════════════════════════════
    // STEALTH ABILITY
    // ═══════════════════════════════════════
    private static void handleStealthAbility(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = player.getEntityWorld().getTime();
        long cooldownEnd = STEALTH_COOLDOWN.getOrDefault(uuid, 0L);

        if (player.isSneaking() && player.getVelocity().lengthSquared() < 0.001) {
            int ticks = SNEAK_TICK_COUNTER.getOrDefault(uuid, 0) + 1;
            SNEAK_TICK_COUNTER.put(uuid, ticks);

            if (ticks >= 40 && now > cooldownEnd) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 60, 0, true, false, true));
                STEALTH_COOLDOWN.put(uuid, now + 600);
                SNEAK_TICK_COUNTER.put(uuid, 0);
            }
        } else {
            SNEAK_TICK_COUNTER.put(uuid, 0);
        }
    }

    // ═══════════════════════════════════════
    // DAMAGE HANDLING
    // ═══════════════════════════════════════
    private static boolean handlePlayerDamaged(ServerPlayerEntity player, DamageSource source, float amount) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return true;

        // Guardian ultimate: Fortress - track hits taken
        if (data.hasUnlockedAbility("guardian_fortress")) {
            UUID uuid = player.getUuid();
            long now = player.getEntityWorld().getTime();
            List<Long> hits = HIT_TAKEN_TIMESTAMPS.computeIfAbsent(uuid, k -> new ArrayList<>());
            hits.add(now);
            hits.removeIf(t -> now - t > 1200); // Remove hits older than 60s
            if (hits.size() >= 10) {
                if (markUltimateReady(player, "Fortress")) {
                    hits.clear();
                }
            }
        }

        // Guardian - Aegis: 20% chance to negate
        if (data.hasUnlockedAbility("guardian_aegis")) {
            if (player.getRandom().nextFloat() < 0.20f) {
                player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK.value(),
                        SoundCategory.PLAYERS, 1.0f, 1.0f);
                return false;
            }
        }

        // Trickster - Evasion: 15% dodge chance
        if (data.hasUnlockedAbility("trickster_dodge")) {
            if (player.getRandom().nextFloat() < 0.15f) {
                player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS, 0.5f, 1.5f);
                return false;
            }
        }

        // Guardian - Undying: Survive fatal blow
        if (data.hasUnlockedAbility("guardian_immortal")) {
            if (player.getHealth() - amount <= 0) {
                long now = player.getEntityWorld().getTime();
                long cooldownEnd = IMMORTAL_COOLDOWN.getOrDefault(player.getUuid(), 0L);
                if (now > cooldownEnd) {
                    player.setHealth(1.0f);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1, true, true, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, true, true, true));
                    player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE,
                            SoundCategory.PLAYERS, 1.0f, 1.0f);
                    IMMORTAL_COOLDOWN.put(player.getUuid(), now + 12000);
                    return false;
                }
            }
        }

        // Necromancer - Dark Resurrection: Survive fatal blow with wither aura
        if (data.hasUnlockedAbility("necro_resurrection")) {
            if (player.getHealth() - amount <= 0) {
                long now = player.getEntityWorld().getTime();
                long cooldownEnd = NECRO_RES_COOLDOWN.getOrDefault(player.getUuid(), 0L);
                if (now > cooldownEnd) {
                    player.setHealth(1.0f);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 2, true, true, true));
                    // Wither aura - apply wither to nearby mobs
                    applyWitherAura(player, 6, 1, 100);
                    player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_AMBIENT,
                            SoundCategory.PLAYERS, 1.0f, 0.5f);
                    player.sendMessage(Text.literal("  Dark Resurrection activated!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
                    NECRO_RES_COOLDOWN.put(player.getUuid(), now + 6000); // 5 min cooldown
                    return false;
                }
            }
        }

        // Trickster - Smoke Bomb: Invisibility + Speed at low HP
        if (data.hasUnlockedAbility("trickster_escape")) {
            if (player.getHealth() - amount <= player.getMaxHealth() * 0.2f && player.getHealth() > player.getMaxHealth() * 0.2f) {
                long now = player.getEntityWorld().getTime();
                long cooldownEnd = SMOKE_BOMB_COOLDOWN.getOrDefault(player.getUuid(), 0L);
                if (now > cooldownEnd) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 100, 0, true, true, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, true, true, true));
                    player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                            SoundCategory.PLAYERS, 1.0f, 1.0f);
                    SMOKE_BOMB_COOLDOWN.put(player.getUuid(), now + 6000);
                }
            }
        }

        return true;
    }

    // ═══════════════════════════════════════
    // ATTACK HANDLING
    // ═══════════════════════════════════════
    private static void handlePlayerAttacking(ServerPlayerEntity player, LivingEntity target, DamageSource source) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;
        boolean directAttack = isDirectPlayerAttack(source, player);

        // Necromancer - Wither Touch: Apply Wither on melee hit
        if (data.hasUnlockedAbility("necro_wither")) {
            if (directAttack) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0, true, true, true));
            }
        }

        // Track arrow hits for archer ultimate
        if (!(target instanceof PlayerEntity) && isPlayerProjectileAttack(source, player)) {
            onProjectileHit(player, target);
        }
    }

    // ═══════════════════════════════════════
    // KILL HANDLING
    // ═══════════════════════════════════════
    private static void handlePlayerKill(ServerPlayerEntity player, LivingEntity killed) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;
        boolean pveKill = isPveKill(killed);

        // Berserker - Bloodlust: Heal 1 heart on kill
        if (data.hasUnlockedAbility("berserker_bloodlust")) {
            player.heal(2.0f);
        }

        // Berserker - Frenzy: Speed on kill
        if (data.hasUnlockedAbility("berserker_frenzy")) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0, true, true, true));
        }

        // Necromancer - Soul Harvest: Bonus XP
        if (data.hasUnlockedAbility("necro_soul_harvest") && pveKill) {
            player.addExperience(5);
        }

        // Enchanter - Arcane Knowledge: Bonus XP on kill
        if (data.hasUnlockedAbility("enchanter_xp_boost") && pveKill) {
            int bonus = data.hasUnlockedAbility("enchanter_grindstone") ? 5 : 3; // Salvage = 50% more
            player.addExperience(bonus);
        }

        // Trickster - Sticky Fingers: Bonus XP and loot is handled in LootModifier
        if (data.hasUnlockedAbility("trickster_pickpocket") && pveKill) {
            player.addExperience(3);
        }

        // ── ULTIMATE TRACKING: Kill-based ──

        // Berserker - Bloodbath: 5 kills in 30s
        if (data.hasUnlockedAbility("berserker_bloodbath") && pveKill) {
            UUID uuid = player.getUuid();
            long now = player.getEntityWorld().getTime();
            List<Long> kills = KILL_TIMESTAMPS.computeIfAbsent(uuid, k -> new ArrayList<>());
            kills.add(now);
            kills.removeIf(t -> now - t > 600); // Remove kills older than 30s
            if (kills.size() >= 5) {
                if (markUltimateReady(player, "Bloodbath")) {
                    kills.clear();
                }
            }
        }

        // Necromancer - Soul Storm: 10 kills total
        if (data.hasUnlockedAbility("necro_soulstorm") && pveKill) {
            UUID uuid = player.getUuid();
            int count = NECRO_KILL_COUNT.getOrDefault(uuid, 0) + 1;
            NECRO_KILL_COUNT.put(uuid, count);
            if (count >= 10) {
                if (markUltimateReady(player, "Soul Storm")) {
                    NECRO_KILL_COUNT.put(uuid, 0);
                }
            }
        }

        // Trickster - Shadow Dance: 5 kills while invisible
        if (data.hasUnlockedAbility("trickster_shadowdance")) {
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                UUID uuid = player.getUuid();
                int count = INVISIBLE_KILLS.getOrDefault(uuid, 0) + 1;
                INVISIBLE_KILLS.put(uuid, count);
                if (count >= 5) {
                    if (markUltimateReady(player, "Shadow Dance")) {
                        INVISIBLE_KILLS.put(uuid, 0);
                    }
                }
            }
        }
    }

    private static void handleProjectileKill(ServerPlayerEntity player, LivingEntity killed) {
        // Arrow kills also count for kill tracking but are handled by handlePlayerKill
        // This is for additional arrow-specific tracking
    }

    // ═══════════════════════════════════════
    // OUTGOING DAMAGE MODIFICATION
    // ═══════════════════════════════════════
    public static float modifyOutgoingDamage(ServerPlayerEntity player, LivingEntity target, DamageSource source, float originalDamage) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return originalDamage;

        float damage = originalDamage;
        boolean directAttack = isDirectPlayerAttack(source, player);
        boolean projectileAttack = isPlayerProjectileAttack(source, player);

        // Berserker - Rage: +20% damage below 50% HP
        if (data.hasUnlockedAbility("berserker_rage") && directAttack) {
            if (player.getHealth() < player.getMaxHealth() * 0.5f) {
                damage *= 1.20f;
            }
        }

        // Berserker - War Cry: +40% damage below 25% HP
        if (data.hasUnlockedAbility("berserker_warcry") && directAttack) {
            if (player.getHealth() < player.getMaxHealth() * 0.25f) {
                damage *= 1.40f;
            }
        }

        // Archer - Steady Aim: +15% arrow damage
        if (data.hasUnlockedAbility("archer_steady")) {
            if (projectileAttack) {
                damage *= 1.15f;
            }
        }

        // Archer - Quick Draw: Partially-charged arrows deal bonus damage
        if (data.hasUnlockedAbility("archer_quickdraw")) {
            if (projectileAttack) {
                damage *= 1.10f; // Flat 10% bonus representing quickdraw advantage
            }
        }

        // Archer - Piercing Shot: 15% chance for +30% armor-piercing damage
        if (data.hasUnlockedAbility("archer_pierce")) {
            if (projectileAttack) {
                if (player.getRandom().nextFloat() < 0.15f) {
                    damage *= 1.30f;
                }
            }
        }

        // Archer - Multi Shot: 10% chance for double damage
        if (data.hasUnlockedAbility("archer_multishot")) {
            if (projectileAttack) {
                if (player.getRandom().nextFloat() < 0.10f) {
                    damage *= 2.0f;
                }
            }
        }

        // Archer - Sniper: +50% damage at long range (>20 blocks)
        if (data.hasUnlockedAbility("archer_sniper") && projectileAttack) {
            double distance = player.distanceTo(target);
            if (distance > 20.0) {
                damage *= 1.50f;
            }
        }

        // Archer - Dead Eye active: double damage
        if (isDeadEyeActive(player.getUuid()) && projectileAttack) {
            damage *= 1.50f;
        }

        // Necromancer - Life Drain: Heal 0.5 hearts on hit
        if (data.hasUnlockedAbility("necro_drain") && directAttack) {
            player.heal(1.0f);
        }

        // Trickster - Backstab: +50% from behind
        if (data.hasUnlockedAbility("trickster_backstab")) {
            double angleDiff = getAngleDifference(player, target);
            if (angleDiff < 60) {
                damage *= 1.50f;
            }
        }

        return damage;
    }

    // ═══════════════════════════════════════
    // INCOMING DAMAGE MODIFICATION
    // ═══════════════════════════════════════
    public static float modifyIncomingDamage(ServerPlayerEntity player, DamageSource source, float originalDamage) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return originalDamage;

        float damage = originalDamage;
        LivingEntity attacker = resolveLivingAttacker(source);

        // Guardian - Iron Skin: 10% damage reduction
        if (data.hasUnlockedAbility("guardian_shield")) {
            damage *= 0.90f;
        }

        // Explorer - Feather Step: 50% less fall damage
        if (data.hasUnlockedAbility("explorer_no_fall")) {
            if (source.isOf(DamageTypes.FALL)) {
                damage *= 0.50f;
            }
        }

        // Necromancer - Undead Pact: 50% less damage from undead
        if (data.hasUnlockedAbility("necro_undead_friend")) {
            if (attacker instanceof ZombieEntity || attacker instanceof SkeletonEntity) {
                damage *= 0.50f;
            }
        }

        // Guardian - Thorns Aura: Reflect 1 heart damage
        if (data.hasUnlockedAbility("guardian_thorns")) {
            if (attacker != null && attacker != player && !source.isOf(DamageTypes.THORNS)) {
                if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
                    attacker.damage(serverWorld, player.getDamageSources().thorns(player), 2.0f);
                }
            }
        }

        return damage;
    }

    // ═══════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════

    private static boolean canActivateUltimate(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = player.getEntityWorld().getTime();
        long cooldownEnd = ULTIMATE_COOLDOWN.getOrDefault(uuid, 0L);
        return now > cooldownEnd;
    }

    private static long getUltimateCooldownRemainingTicks(ServerPlayerEntity player) {
        long now = player.getEntityWorld().getTime();
        long cooldownEnd = ULTIMATE_COOLDOWN.getOrDefault(player.getUuid(), 0L);
        return Math.max(0L, cooldownEnd - now);
    }

    private static void showUltimateCooldownActionbar(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long remainingTicks = getUltimateCooldownRemainingTicks(player);
        if (remainingTicks <= 0L) {
            LAST_ULTIMATE_COOLDOWN_ACTIONBAR.remove(uuid);
            return;
        }

        long secondsRemaining = (remainingTicks + 19L) / 20L;
        Long lastShown = LAST_ULTIMATE_COOLDOWN_ACTIONBAR.get(uuid);
        if (lastShown != null && lastShown == secondsRemaining) {
            return;
        }

        LAST_ULTIMATE_COOLDOWN_ACTIONBAR.put(uuid, secondsRemaining);
        player.sendMessage(Text.literal("Ultimate CD: ").formatted(Formatting.DARK_PURPLE)
                .append(Text.literal(formatUltimateCooldown(remainingTicks)).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)), true);
    }

    private static String formatUltimateCooldown(long remainingTicks) {
        long totalSeconds = (remainingTicks + 19L) / 20L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static boolean markUltimateReady(ServerPlayerEntity player, String name) {
        UUID uuid = player.getUuid();
        if (!canActivateUltimate(player)) return false;
        if (ULTIMATE_READY.containsKey(uuid)) return false;
        ULTIMATE_READY.put(uuid, name);

        player.sendMessage(Text.literal("Ultimate READY: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" (press ").formatted(Formatting.GRAY))
                .append(Text.keybind("key.endlessfortune.activate_ultimate").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(")").formatted(Formatting.GRAY)), true);
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_IN, SoundCategory.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    public static void tryActivateReadyUltimate(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        String name = ULTIMATE_READY.get(uuid);
        if (name == null) {
            player.sendMessage(Text.literal("No ultimate is READY.").formatted(Formatting.GRAY), true);
            return;
        }
        if (!canActivateUltimate(player)) {
            ULTIMATE_READY.remove(uuid);
            player.sendMessage(Text.literal("Ultimate not available (cooldown).").formatted(Formatting.RED), true);
            return;
        }

        if (!applyUltimate(player, name)) {
            return;
        }

        ULTIMATE_READY.remove(uuid);
        activateUltimate(player, name);
    }

    public static void onPotionConsumed(ServerPlayerEntity player, ItemStack consumedPotion) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;

        if (data.hasUnlockedAbility("alchemist_double_brew")
                && player.getRandom().nextFloat() < 0.25f
                && modifyConsumedPotionEffects(player, consumedPotion, 2.0, 0)) {
            player.sendMessage(Text.literal("Double Brew!").formatted(Formatting.AQUA, Formatting.ITALIC), true);
        }

        if (data.hasUnlockedAbility("alchemist_philosopher")) {
            modifyConsumedPotionEffects(player, consumedPotion, 1.0, 1);
        }

        if (data.hasUnlockedAbility("alchemist_elixir")) {
            UUID uuid = player.getUuid();
            long now = player.getEntityWorld().getTime();
            List<Long> uses = ALCHEMIST_POTION_TIMESTAMPS.computeIfAbsent(uuid, key -> new ArrayList<>());
            uses.add(now);
            uses.removeIf(time -> now - time > 2400);

            if (uses.size() >= 5 && markUltimateReady(player, "Elixir Master")) {
                uses.clear();
            }
        }
    }

    public static void onSplashPotionEffect(ServerPlayerEntity player) {
        PlayerData data = PlayerDataManager.get(player);
        if (data.getSkill() == null) return;

        if (data.hasUnlockedAbility("alchemist_splash_range")) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, true, true, true));
        }
    }

    private static boolean applyUltimate(ServerPlayerEntity player, String name) {
        UUID uuid = player.getUuid();
        long now = player.getEntityWorld().getTime();

        switch (name) {
            case "Wind Walker" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 600, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0, true, true, true));
                return true;
            }
            case "Poseidon's Blessing" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 1200, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 1200, 0, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 1200, 0, true, true, true));
                return true;
            }
            case "Arcane Surge" -> {
                if (!player.isSneaking() || player.experienceLevel < 30) {
                    player.sendMessage(Text.literal("Arcane Surge requires sneaking with 30+ levels.").formatted(Formatting.RED), true);
                    return false;
                }
                player.addExperienceLevels(-10);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 1200, 1, true, true, true));
                ARCANE_SURGE_ACTIVE.put(uuid, now + 1200);
                return true;
            }
            case "Dead Eye" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, true, true, true));
                DEAD_EYE_ACTIVE.put(uuid, now + 200);
                return true;
            }
            case "Motherlode" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, true, true, true));
                MOTHERLODE_ACTIVE.put(uuid, now + 600);
                return true;
            }
            case "Timber Storm" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1, true, true, true));
                return true;
            }
            case "Harvest Moon" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1200, 1, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 1200, 0, true, true, true));
                growNearbyCrops(player, 8);
                growNearbyCrops(player, 8);
                return true;
            }
            case "Fortress" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 4, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1, true, true, true));
                return true;
            }
            case "Bloodbath" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 2, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 1, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 300, 0, true, true, true));
                return true;
            }
            case "Soul Storm" -> {
                applyWitherAura(player, 8, 1, 200);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2, true, true, true));
                return true;
            }
            case "Elixir Master" -> {
                if (!refreshBeneficialEffects(player, 2.0, 0, 600)) {
                    player.sendMessage(Text.literal("Elixir Master needs an active positive effect.").formatted(Formatting.RED), true);
                    return false;
                }
                return true;
            }
            case "Shadow Dance" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 300, 0, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 1, true, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 0, true, true, true));
                return true;
            }
            default -> {
                player.sendMessage(Text.literal("Unknown ultimate: " + name).formatted(Formatting.RED), true);
                return false;
            }
        }
    }

    private static void activateUltimate(ServerPlayerEntity player, String name) {
        UUID uuid = player.getUuid();
        ULTIMATE_COOLDOWN.put(uuid, player.getEntityWorld().getTime() + ULTIMATE_GLOBAL_COOLDOWN);
        showUltimateCooldownActionbar(player);

        player.sendMessage(Text.literal("ULTIMATE: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD)), true);
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.PLAYERS, 1.0f, 1.2f);
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                SoundCategory.PLAYERS, 0.3f, 1.5f);
    }

    private static ServerPlayerEntity resolveAttackingPlayer(DamageSource source) {
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            return player;
        }
        if (source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ServerPlayerEntity player) {
            return player;
        }
        return null;
    }

    private static LivingEntity resolveLivingAttacker(DamageSource source) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            return attacker;
        }
        if (source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() instanceof LivingEntity attacker) {
            return attacker;
        }
        return null;
    }

    private static boolean isPlayerCombatHit(DamageSource source, ServerPlayerEntity player) {
        return isDirectPlayerAttack(source, player) || isPlayerProjectileAttack(source, player);
    }

    private static void trackPlayerProjectileHit(ServerPlayerEntity attacker, ServerPlayerEntity targetPlayer, DamageSource source) {
        if (attacker == targetPlayer) {
            return;
        }
        if (isPlayerProjectileAttack(source, attacker)) {
            onProjectileHit(attacker, targetPlayer);
        }
    }

    private static boolean isDirectPlayerAttack(DamageSource source, ServerPlayerEntity player) {
        return source.isOf(DamageTypes.PLAYER_ATTACK) && source.getAttacker() == player;
    }

    private static boolean isPlayerProjectileAttack(DamageSource source, ServerPlayerEntity player) {
        return source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() == player;
    }

    private static boolean isPveKill(LivingEntity killed) {
        return !(killed instanceof PlayerEntity);
    }

    private static void dropPotionOfFortuneOnPvpDeath(ServerPlayerEntity deadPlayer) {
        PlayerData data = PlayerDataManager.get(deadPlayer);
        if (data.getLuck() < POTION_OF_FORTUNE_LUCK_COST) {
            return;
        }

        data.setLuck(data.getLuck() - POTION_OF_FORTUNE_LUCK_COST);
        data.checkAndUnlockAbilities();
        PlayerDataManager.save(deadPlayer.getUuid());
        deadPlayer.dropItem(new ItemStack(ModItems.POTION_OF_FORTUNE), true, false);
        deadPlayer.sendMessage(Text.literal("Dropped Potion of Fortune on death (-20% Luck)").formatted(Formatting.RED), false);
    }

    private static void expireTimedUltimateStates(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = player.getEntityWorld().getTime();

        if (DEAD_EYE_ACTIVE.getOrDefault(uuid, 0L) <= now) {
            DEAD_EYE_ACTIVE.remove(uuid);
        }
        if (MOTHERLODE_ACTIVE.getOrDefault(uuid, 0L) <= now) {
            MOTHERLODE_ACTIVE.remove(uuid);
        }
    }

    private static void applyWitherAura(ServerPlayerEntity player, int radius, int amplifier, int duration) {
        Box box = player.getBoundingBox().expand(radius);
        List<LivingEntity> entities = player.getEntityWorld().getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e instanceof HostileEntity);
        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, amplifier, true, true, true));
        }
    }

    private static boolean modifyConsumedPotionEffects(ServerPlayerEntity player, ItemStack consumedPotion, double durationMultiplier, int amplifierBonus) {
        PotionContentsComponent potionContents = consumedPotion.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        float durationScale = consumedPotion.getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0f);
        List<StatusEffectInstance> consumedEffects = new ArrayList<>();
        potionContents.forEachEffect(consumedEffects::add, durationScale);
        boolean applied = false;

        for (StatusEffectInstance instance : consumedEffects) {
            if (!instance.getEffectType().value().isBeneficial()) {
                continue;
            }

            StatusEffectInstance activeInstance = player.getStatusEffect(instance.getEffectType());
            if (activeInstance == null) {
                continue;
            }

            int newDuration = Math.max(activeInstance.getDuration(), Math.max(20, (int) Math.round(instance.getDuration() * durationMultiplier)));
            int newAmplifier = Math.max(activeInstance.getAmplifier(), Math.max(0, instance.getAmplifier() + amplifierBonus));
            if (newDuration == activeInstance.getDuration() && newAmplifier == activeInstance.getAmplifier()) {
                continue;
            }

            player.addStatusEffect(new StatusEffectInstance(instance.getEffectType(), newDuration, newAmplifier,
                    activeInstance.isAmbient(), activeInstance.shouldShowParticles(), activeInstance.shouldShowIcon()));
            applied = true;
        }

        return applied;
    }

    private static void modifyBeneficialEffects(ServerPlayerEntity player, double durationMultiplier, int amplifierBonus) {
        List<StatusEffectInstance> activeEffects = new ArrayList<>(player.getActiveStatusEffects().values());

        for (StatusEffectInstance instance : activeEffects) {
            if (!instance.getEffectType().value().isBeneficial()) {
                continue;
            }

            int newDuration = Math.max(20, (int) Math.round(instance.getDuration() * durationMultiplier));
            int newAmplifier = Math.max(0, instance.getAmplifier() + amplifierBonus);
            player.addStatusEffect(new StatusEffectInstance(instance.getEffectType(), newDuration, newAmplifier,
                    instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
        }
    }

    private static boolean refreshBeneficialEffects(ServerPlayerEntity player, double durationMultiplier, int amplifierBonus, int minimumDuration) {
        List<StatusEffectInstance> activeEffects = new ArrayList<>(player.getActiveStatusEffects().values());
        boolean applied = false;

        for (StatusEffectInstance instance : activeEffects) {
            if (!instance.getEffectType().value().isBeneficial()) {
                continue;
            }

            int newDuration = Math.max(minimumDuration, (int) Math.round(instance.getDuration() * durationMultiplier));
            int newAmplifier = Math.max(0, instance.getAmplifier() + amplifierBonus);
            player.addStatusEffect(new StatusEffectInstance(instance.getEffectType(), newDuration, newAmplifier,
                    instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
            applied = true;
        }

        return applied;
    }

    private static void growNearbyCrops(ServerPlayerEntity player, int radius) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;
        BlockPos center = player.getBlockPos();
        net.minecraft.util.math.random.Random random = world.getRandom();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop) {
                        if (!crop.isMature(state)) {
                            if (random.nextFloat() < 0.1f) { // 10% chance per crop per tick
                                crop.grow(world, random, pos, state);
                            }
                        }
                    }
                }
            }
        }
    }

    private static double getAngleDifference(PlayerEntity attacker, LivingEntity target) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();
        double attackerAngle = Math.toDegrees(Math.atan2(dz, dx));
        double targetYaw = target.getYaw();
        double diff = Math.abs(attackerAngle - targetYaw) % 360;
        if (diff > 180) diff = 360 - diff;
        return diff;
    }
}
