package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemCardBoom extends ItemComputronics {
    public ItemCardBoom(Properties properties) {
        super(properties, line("tooltip.computronics.card_boom.desc", ChatFormatting.RED));
    }
}
