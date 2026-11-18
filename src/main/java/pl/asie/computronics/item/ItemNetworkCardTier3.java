package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemNetworkCardTier3 extends ItemComputronics {
    public ItemNetworkCardTier3(Properties properties) {
        super(properties,
                line("tooltip.computronics.network_card_3.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.network_card_3.desc2", ChatFormatting.DARK_AQUA),
                line("tooltip.computronics.network_card_3.desc3", ChatFormatting.YELLOW));
    }
}
