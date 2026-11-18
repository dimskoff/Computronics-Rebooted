package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemSpeechUpgrade extends ItemComputronics {
    public ItemSpeechUpgrade(Properties properties) {
        super(properties, line("tooltip.computronics.upgrade.speech", ChatFormatting.GRAY));
    }
}
