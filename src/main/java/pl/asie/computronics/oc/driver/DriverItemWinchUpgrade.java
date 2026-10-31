package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.item.ItemWinchUpgrade;
import pl.asie.computronics.oc.environment.EnvironmentDroneWinch;

public class DriverItemWinchUpgrade implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemWinchUpgrade;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new EnvironmentDroneWinch(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return 1;
    }
}
