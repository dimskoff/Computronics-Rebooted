package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemNetworkCardTier3 extends Item {
    public ItemNetworkCardTier3(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        tooltip.add(Component.translatable("tooltip.computronics.network_card_3.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.network_card_3.desc2").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.computronics.network_card_3.desc3").withStyle(ChatFormatting.YELLOW));
    }
}
