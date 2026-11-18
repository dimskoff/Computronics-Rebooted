package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemCardBeep extends ItemComputronics {
    public ItemCardBeep(Properties properties) {
        super(properties, line("tooltip.computronics.card_beep.desc", ChatFormatting.GRAY));
    }
}
