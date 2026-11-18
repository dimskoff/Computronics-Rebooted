package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemWinchUpgrade extends ItemComputronics {
    public ItemWinchUpgrade(Properties properties) {
        super(properties, line("tooltip.computronics.upgrade.winch", ChatFormatting.GRAY));
    }
}
