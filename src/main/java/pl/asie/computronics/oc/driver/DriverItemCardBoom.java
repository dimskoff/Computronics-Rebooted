package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.item.ItemCardBoom;
import pl.asie.computronics.oc.environment.EnvironmentCardBoom;

public class DriverItemCardBoom implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCardBoom;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new EnvironmentCardBoom(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        return 1;
    }
}
