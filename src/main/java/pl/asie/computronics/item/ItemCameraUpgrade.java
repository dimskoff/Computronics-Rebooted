package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemCameraUpgrade extends ItemComputronics {
    public ItemCameraUpgrade(Properties properties) {
        super(properties, line("tooltip.computronics.upgrade.camera", ChatFormatting.GRAY));
    }
}
