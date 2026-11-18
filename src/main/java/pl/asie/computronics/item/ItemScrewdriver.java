package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Control panel tool. All panel interaction lives in BlockControlPanel:
 * right-click = settings screen, sneak + right-click = resize, left-click = remove component.
 */
public class ItemScrewdriver extends Item {
    public ItemScrewdriver(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.screwdriver.desc3").withStyle(ChatFormatting.GRAY));
    }
}
