package pl.asie.computronics.item.rack;

import net.minecraft.ChatFormatting;
import pl.asie.computronics.item.ItemComputronics;

public class ItemRackCapacitorBoard extends ItemComputronics {
    public ItemRackCapacitorBoard(Properties properties) {
        super(properties,
                line("tooltip.computronics.rack_board_capacitor.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.rack_board_capacitor.desc2", ChatFormatting.DARK_GRAY));
    }
}
