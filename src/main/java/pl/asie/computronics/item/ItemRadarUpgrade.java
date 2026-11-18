package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemRadarUpgrade extends ItemComputronics {
    public ItemRadarUpgrade(Properties properties) {
        super(properties, line("tooltip.computronics.upgrade.radar", ChatFormatting.GRAY));
    }
}
