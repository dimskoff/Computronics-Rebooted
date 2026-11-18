package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemCardFX extends ItemComputronics {
    public ItemCardFX(Properties properties) {
        super(properties, line("tooltip.computronics.card_fx.desc", ChatFormatting.LIGHT_PURPLE));
    }
}
