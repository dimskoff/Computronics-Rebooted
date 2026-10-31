package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.item.ItemTape;

public class DriverItemTape implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemTape;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Floppy;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof ItemTape tape) {
            if (tape.getCapacity() >= ItemTape.CAPACITY_NETHER_STAR) return 3;
            if (tape.getCapacity() >= ItemTape.CAPACITY_DIAMOND) return 2;
            if (tape.getCapacity() >= ItemTape.CAPACITY_GOLD) return 1;
        }
        return 0;
    }
}
