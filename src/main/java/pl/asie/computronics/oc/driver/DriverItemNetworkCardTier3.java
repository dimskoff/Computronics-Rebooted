package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.item.ItemNetworkCardTier3;
import pl.asie.computronics.oc.environment.EnvironmentNetworkCardTier3;

public class DriverItemNetworkCardTier3 implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemNetworkCardTier3;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new EnvironmentNetworkCardTier3(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        return 2; // Tier 3 is index 2
    }
}
