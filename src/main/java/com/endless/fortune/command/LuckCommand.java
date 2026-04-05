package com.endless.fortune.command;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillAbility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class LuckCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("luck")
                .executes(LuckCommand::showLuck)
                .then(CommandManager.literal("set")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(LuckCommand::setLuck))))
                .then(CommandManager.literal("add")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(-100.0, 100.0))
                                        .executes(LuckCommand::addLuck))))
                .then(CommandManager.literal("info")
                        .executes(LuckCommand::showDetailedInfo))
        );
    }

    private static int showLuck(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            PlayerData data = PlayerDataManager.get(player);
            Skill skill = data.getSkill();

            MutableText msg = Text.literal("Luck: ").formatted(Formatting.GOLD)
                    .append(Text.literal(String.format("%.1f%%", data.getLuck())).formatted(getLuckColor(data.getLuck()), Formatting.BOLD));
            if (skill != null) {
                msg = msg.append(Text.literal(" | Skill: ").formatted(Formatting.GRAY))
                        .append(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD))
                        .append(Text.literal(" | Abilities: ").formatted(Formatting.GRAY))
                        .append(Text.literal(data.getUnlockedAbilities().size() + "/" + skill.getAbilities().size()).formatted(Formatting.WHITE));
            }
            player.sendMessage(msg, true);
        }
        return 1;
    }

    private static int showDetailedInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            PlayerData data = PlayerDataManager.get(player);
            Skill skill = data.getSkill();

            player.sendMessage(Text.literal("═══════════════════════════════════════").formatted(Formatting.GOLD));
            player.sendMessage(Text.literal("  ★ Detailed Luck Info ★").formatted(Formatting.GOLD, Formatting.BOLD));
            player.sendMessage(Text.literal("═══════════════════════════════════════").formatted(Formatting.GOLD));
            player.sendMessage(Text.literal("  Luck: ").formatted(Formatting.YELLOW)
                    .append(Text.literal(String.format("%.1f%%", data.getLuck())).formatted(getLuckColor(data.getLuck()), Formatting.BOLD)));

            if (skill != null) {
                player.sendMessage(Text.literal(""));
                player.sendMessage(Text.literal("  Skill: ").formatted(Formatting.YELLOW)
                        .append(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD))
                        .append(Text.literal(" [" + skill.getCategory().getDisplayName() + "]").formatted(skill.getCategory().getColor())));
                player.sendMessage(Text.literal("  " + skill.getDescription()).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                player.sendMessage(Text.literal(""));
                player.sendMessage(Text.literal("  Abilities:").formatted(Formatting.AQUA, Formatting.BOLD));

                for (SkillAbility ability : skill.getAbilities()) {
                    boolean unlocked = data.hasUnlockedAbility(ability.id());
                    if (unlocked) {
                        player.sendMessage(Text.literal("    ✓ ").formatted(Formatting.GREEN)
                                .append(Text.literal(ability.name()).formatted(Formatting.WHITE, Formatting.BOLD))
                                .append(Text.literal(String.format(" (%.0f%%)", ability.requiredLuck())).formatted(Formatting.DARK_GRAY)));
                    } else {
                        player.sendMessage(Text.literal("    ✗ ").formatted(Formatting.RED)
                                .append(Text.literal(ability.name()).formatted(Formatting.GRAY))
                                .append(Text.literal(String.format(" (Requires %.0f%% Luck)", ability.requiredLuck())).formatted(Formatting.DARK_GRAY)));
                    }
                    player.sendMessage(Text.literal("      " + ability.description()).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                }
            }

            player.sendMessage(Text.literal("═══════════════════════════════════════").formatted(Formatting.GOLD));
            player.sendMessage(Text.literal("  Loot Upgrade Chance: ").formatted(Formatting.YELLOW)
                    .append(Text.literal(String.format("%.1f%%", data.getLuck() / 2.0)).formatted(Formatting.LIGHT_PURPLE)));
            player.sendMessage(Text.literal("═══════════════════════════════════════").formatted(Formatting.GOLD));
        }
        return 1;
    }

    private static int setLuck(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            PlayerData data = PlayerDataManager.get(target);
            data.setLuck(amount);
            data.checkAndUnlockAbilities();
            PlayerDataManager.save(target.getUuid());

            context.getSource().sendMessage(Text.literal("Set " + target.getName().getString() + "'s luck to " + String.format("%.1f%%", amount))
                    .formatted(Formatting.GREEN));
            target.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Your luck has been set to ").formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format("%.1f%%", amount)).formatted(Formatting.GREEN, Formatting.BOLD)));
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
        }
        return 1;
    }

    private static int addLuck(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            PlayerData data = PlayerDataManager.get(target);
            double oldLuck = data.getLuck();
            data.addLuck(amount);
            data.checkAndUnlockAbilities();
            PlayerDataManager.save(target.getUuid());

            context.getSource().sendMessage(Text.literal("Added " + String.format("%.1f%%", amount) + " luck to " +
                    target.getName().getString() + " (" + String.format("%.1f%% → %.1f%%", oldLuck, data.getLuck()) + ")")
                    .formatted(Formatting.GREEN));
            target.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal(String.format("%.1f%% luck added! ", amount)).formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format("(%.1f%% → %.1f%%)", oldLuck, data.getLuck())).formatted(Formatting.GRAY)));
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
        }
        return 1;
    }

    private static Formatting getLuckColor(double luck) {
        if (luck >= 75) return Formatting.LIGHT_PURPLE;
        if (luck >= 50) return Formatting.GOLD;
        if (luck >= 25) return Formatting.YELLOW;
        if (luck >= 10) return Formatting.GREEN;
        return Formatting.GRAY;
    }
}
