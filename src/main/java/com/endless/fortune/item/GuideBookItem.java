package com.endless.fortune.item;

import com.endless.fortune.skill.Skill;
import com.endless.fortune.skill.SkillAbility;
import com.endless.fortune.skill.SkillCategory;
import com.endless.fortune.skill.SkillManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class GuideBookItem extends Item {

    public GuideBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, createGuideBookContent());
        user.useBook(stack, hand);
        return ActionResult.SUCCESS;
    }

    private static WrittenBookContentComponent createGuideBookContent() {
        return new WrittenBookContentComponent(
                RawFilteredPair.of("Endless Fortune Guide"),
                "Endless Fortune",
                0,
                buildPages(),
                false);
    }

    private static List<RawFilteredPair<Text>> buildPages() {
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        pages.add(page(Text.empty()
                .append(Text.literal("Endless Fortune Guide").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\n\nOpen this book anytime for skills, items, and controls.").formatted(Formatting.GRAY))
                .append(Text.literal("\n\nUltimate Key: ").formatted(Formatting.YELLOW))
                .append(Text.keybind("key.endlessfortune.activate_ultimate").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal("\nWhen an ultimate becomes READY, press that key to activate it.").formatted(Formatting.GRAY))
                .append(Text.literal("\n\nCommands:\n/skill\n/skill info <skillId>\n/luck\n/withdraw").formatted(Formatting.DARK_GRAY))));

        for (SkillCategory category : SkillCategory.values()) {
            pages.add(page(Text.literal(category.getDisplayName()).formatted(category.getColor(), Formatting.BOLD)
                    .append(Text.literal("\n\nSkills in this category:").formatted(Formatting.GRAY))));

            for (Skill skill : SkillManager.getSkillsByCategory(category)) {
                pages.add(buildSkillPage(skill));
            }
        }

        pages.add(page(Text.empty()
                .append(Text.literal("Items & Systems").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\n\nPotion of Regret\n").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal("Rerolls your current skill.\n\n").formatted(Formatting.GRAY))
                .append(Text.literal("Potion of Fortune\n").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal("Restores 20% luck.\n\n").formatted(Formatting.GRAY))
                .append(Text.literal("Skill Tomes\n").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal("Swap to a Combat, Utility, or Gathering skill and reset luck to 0%.\n\n").formatted(Formatting.GRAY))
                .append(Text.literal("Guide Book\n").formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal("Opens this book again whenever you need a reminder.").formatted(Formatting.GRAY))));

        return pages;
    }

    private static RawFilteredPair<Text> buildSkillPage(Skill skill) {
        MutableText page = Text.empty()
                .append(Text.literal(skill.getName()).formatted(skill.getCategory().getColor(), Formatting.BOLD))
                .append(Text.literal("\n" + skill.getCategory().getDisplayName()).formatted(Formatting.DARK_GRAY))
                .append(Text.literal("\n\n" + skill.getDescription()).formatted(Formatting.GRAY));

        for (SkillAbility ability : skill.getAbilities()) {
            page = page.append(Text.literal("\n\n" + String.format("%.0f%% ", ability.requiredLuck())).formatted(Formatting.GOLD, Formatting.BOLD))
                    .append(Text.literal(ability.name()).formatted(Formatting.YELLOW))
                    .append(Text.literal("\n" + ability.description()).formatted(Formatting.GRAY));
        }
        return page(page);
    }

    private static RawFilteredPair<Text> page(Text text) {
        return RawFilteredPair.of(text);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
