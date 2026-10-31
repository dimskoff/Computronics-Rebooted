package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.item.ItemPacketSniffer;
import pl.asie.computronics.oc.environment.EnvironmentPacketSniffer;

public class DriverItemPacketSniffer implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPacketSniffer;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new EnvironmentPacketSniffer(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        return 1; // Tier 2 card
    }
}
