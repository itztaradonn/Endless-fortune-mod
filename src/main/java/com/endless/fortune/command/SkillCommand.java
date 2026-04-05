package com.endless.fortune.command;

import com.endless.fortune.data.PlayerData;
import com.endless.fortune.data.PlayerDataManager;
import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillCategory;
import com.endless.fortune.skill.SkillManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkillCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("skill")
                .executes(SkillCommand::showSkill)
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("skillId", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    SkillManager.getAllSkills().forEach(s -> builder.suggest(s.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(SkillCommand::showSkillInfo)))
                .then(CommandManager.literal("list")
                        .executes(SkillCommand::listSkills))
                .then(CommandManager.literal("set")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("skillId", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            SkillManager.getAllSkills().forEach(s -> builder.suggest(s.getId()));
                                            return builder.buildFuture();
                                        })
                                        .executes(SkillCommand::setSkill))))
        );
    }

    private static int showSkill(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            PlayerData data = PlayerDataManager.get(player);
            Skill skill = data.getSkill();

            if (skill == null) {
                player.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                        .append(Text.literal("You don't have a skill yet! Rejoin the server.").formatted(Formatting.RED)));
                return 0;
            }

            player.sendMessage(Text.literal("Your Skill: ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD))
                    .append(Text.literal(" [" + skill.getCategory().getDisplayName() + "]").formatted(skill.getCategory().getColor())));
            player.sendMessage(Text.literal(skill.getDescription()).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            player.sendMessage(Text.literal("Abilities:").formatted(Formatting.AQUA, Formatting.BOLD));

            skill.getAbilities().forEach(ability -> {
                boolean unlocked = data.hasUnlockedAbility(ability.id());
                Formatting nameColor = unlocked ? Formatting.WHITE : Formatting.GRAY;
                String prefix = unlocked ? "✓ " : "✗ ";
                Formatting prefixColor = unlocked ? Formatting.GREEN : Formatting.RED;

                player.sendMessage(Text.literal(prefix).formatted(prefixColor)
                        .append(Text.literal(ability.name()).formatted(nameColor, unlocked ? Formatting.BOLD : Formatting.ITALIC))
                        .append(Text.literal(String.format(" (%.0f%%)", ability.requiredLuck())).formatted(Formatting.DARK_GRAY)));
            });
        }
        return 1;
    }

    private static int showSkillInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            String skillId = StringArgumentType.getString(context, "skillId");
            Skill skill = SkillManager.getSkill(skillId);
            if (skill == null) {
                player.sendMessage(Text.literal("Unknown skill: ").formatted(Formatting.RED)
                        .append(Text.literal(skillId).formatted(Formatting.YELLOW)), false);
                return 0;
            }

            player.sendMessage(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD)
                    .append(Text.literal(" [" + skill.getCategory().getDisplayName() + "]").formatted(skill.getCategory().getColor())), false);
            skill.getAbilities().forEach(ability -> player.sendMessage(
                    Text.literal(String.format("%.0f%% ", ability.requiredLuck())).formatted(Formatting.GOLD)
                            .append(Text.literal(ability.name()).formatted(Formatting.YELLOW))
                            .append(Text.literal(": ").formatted(Formatting.DARK_GRAY))
                            .append(Text.literal(ability.description()).formatted(Formatting.GRAY)),
                    false));
        }
        return 1;
    }

    private static int listSkills(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.literal("All Skills:").formatted(Formatting.GOLD, Formatting.BOLD));

            for (SkillCategory category : SkillCategory.values()) {
                player.sendMessage(Text.literal(category.getDisplayName()).formatted(category.getColor(), Formatting.BOLD, Formatting.UNDERLINE));

                for (Skill skill : SkillManager.getSkillsByCategory(category)) {
                    Text skillText = Text.literal("- ").formatted(category.getColor())
                            .append(Text.literal(skill.getName()).formatted(Formatting.WHITE)
                                    .styled(style -> style
                                            .withHoverEvent(new HoverEvent.ShowText(
                                                    Text.literal(skill.getDescription() + "\n\nAbilities: " + skill.getAbilities().size())
                                                            .formatted(Formatting.GRAY)))));
                    player.sendMessage(skillText);
                }
            }
        }
        return 1;
    }

    private static int setSkill(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            String skillId = StringArgumentType.getString(context, "skillId");
            Skill skill = SkillManager.getSkill(skillId);

            if (skill == null) {
                context.getSource().sendError(Text.literal("Unknown skill: " + skillId));
                return 0;
            }

            PlayerData data = PlayerDataManager.get(target);
            data.setSkillId(skillId);
            data.checkAndUnlockAbilities();
            PlayerDataManager.save(target.getUuid());

            context.getSource().sendMessage(Text.literal("Set " + target.getName().getString() + "'s skill to " + skill.getName())
                    .formatted(Formatting.GREEN));
            target.sendMessage(Text.literal("[Endless Fortune] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Your skill has been set to ").formatted(Formatting.YELLOW))
                    .append(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD)));
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
}
