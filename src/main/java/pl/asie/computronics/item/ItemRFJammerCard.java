package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemRFJammerCard extends ItemComputronics {
    public ItemRFJammerCard(Properties properties) {
        super(properties,
                line("tooltip.computronics.rf_jammer_card.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.rf_jammer_card.desc2", ChatFormatting.DARK_GRAY));
    }
}
