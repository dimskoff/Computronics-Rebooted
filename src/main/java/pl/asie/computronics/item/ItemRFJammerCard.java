package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemRFJammerCard extends Item {
    public ItemRFJammerCard(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.computronics.rf_jammer_card.desc1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.computronics.rf_jammer_card.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
