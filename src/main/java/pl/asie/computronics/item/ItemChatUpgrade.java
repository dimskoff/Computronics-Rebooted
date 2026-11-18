package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemChatUpgrade extends ItemComputronics {
    public ItemChatUpgrade(Properties properties) {
        super(properties, line("tooltip.computronics.upgrade.chat", ChatFormatting.GRAY));
    }
}
