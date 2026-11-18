package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base class for simple, non-stackable Computronics items (cards, upgrades, rack boards)
 * whose only client-side behaviour is a fixed, translated tooltip.
 */
public class ItemComputronics extends Item {
    /** One translated tooltip line. */
    public record TooltipLine(String key, ChatFormatting style) {}

    private final List<TooltipLine> tooltipLines;

    public ItemComputronics(Properties properties, TooltipLine... tooltipLines) {
        super(properties.stacksTo(1));
        this.tooltipLines = List.of(tooltipLines);
    }

    protected static TooltipLine line(String key, ChatFormatting style) {
        return new TooltipLine(key, style);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        for (TooltipLine line : tooltipLines) {
            tooltip.add(Component.translatable(line.key()).withStyle(line.style()));
        }
    }
}
