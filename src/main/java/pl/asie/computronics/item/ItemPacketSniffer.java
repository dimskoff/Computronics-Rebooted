package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;

public class ItemPacketSniffer extends ItemComputronics {
    public ItemPacketSniffer(Properties properties) {
        super(properties,
                line("tooltip.computronics.packet_sniffer.desc1", ChatFormatting.GRAY),
                line("tooltip.computronics.packet_sniffer.desc2", ChatFormatting.GREEN));
    }
}
