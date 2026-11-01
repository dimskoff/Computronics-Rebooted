package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.init.ModItems;

public class DriverRackMountable implements DriverItem {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack.getItem() == ModItems.RACK_BOARD_LIGHT.get()
                || stack.getItem() == ModItems.RACK_BOARD_SWITCH.get()
                || stack.getItem() == ModItems.RACK_BOARD_BOOM.get()
                || stack.getItem() == ModItems.RACK_BOARD_CAPACITOR.get()
                || stack.getItem() == ModItems.RACK_BOARD_CO_PROCESSOR.get();
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (stack.getItem() == ModItems.RACK_BOARD_LIGHT.get()) {
            return new EnvironmentRackLightBoard(host, pl.asie.computronics.item.rack.ItemRackLightBoard.getMode(stack));
        } else if (stack.getItem() == ModItems.RACK_BOARD_SWITCH.get()) {
            return new EnvironmentRackSwitchBoard(host);
        } else if (stack.getItem() == ModItems.RACK_BOARD_BOOM.get()) {
            return new EnvironmentRackBoomBoard(host);
        } else if (stack.getItem() == ModItems.RACK_BOARD_CAPACITOR.get()) {
            return new EnvironmentRackCapacitorBoard(host);
        } else if (stack.getItem() == ModItems.RACK_BOARD_CO_PROCESSOR.get()) {
            return new EnvironmentRackCoProcessor(host);
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.RackMountable;
    }

    @Override
    public int tier(ItemStack stack) {
        return 1;
    }
}
