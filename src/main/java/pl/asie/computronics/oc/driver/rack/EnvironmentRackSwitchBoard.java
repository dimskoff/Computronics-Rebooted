package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class EnvironmentRackSwitchBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final boolean[] switches = new boolean[4];

    public EnvironmentRackSwitchBoard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("switch_board", Visibility.Network)
                .create());
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Gets state of a switch (0-3).")
    public Object[] getSwitch(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < switches.length) {
            return new Object[]{switches[index]};
        }
        return new Object[]{null, "index out of range"};
    }

    @Callback(doc = "function(index:number, state:boolean):boolean -- Sets state of a switch (0-3).")
    public Object[] setSwitch(Context context, Arguments args) {
        int index = args.checkInteger(0);
        boolean state = args.checkBoolean(1);
        if (index >= 0 && index < switches.length) {
            switches[index] = state;
            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
            if (node() != null) {
                node().sendToReachable("computer.signal", "rack_switch", index, state);
            }
            return new Object[]{true};
        }
        return new Object[]{false, "index out of range"};
    }

    @Callback(direct = true, doc = "function():number -- Gets total number of switches (4).")
    public Object[] getSwitchCount(Context context, Arguments args) {
        return new Object[]{switches.length};
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Input,
                DeviceInfo.DeviceAttribute.Description, "Rack Switch Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "SwitchBoard-4"
        );
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack stack, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            int swIdx = Math.max(0, Math.min(3, (int) Math.floor(hitX * 4.0f)));
            switches[swIdx] = !switches[swIdx];
            float pitch = switches[swIdx] ? 1.0f : 0.8f;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, pitch);

            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
            if (node() != null) {
                node().sendToReachable("computer.signal", "rack_switch", swIdx, switches[swIdx]);
            }
        }
        return true;
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(tag, registries);
        for (int i = 0; i < switches.length; i++) {
            if (tag.contains("switch_" + i)) {
                switches[i] = tag.getBoolean("switch_" + i);
            }
        }
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveData(tag, registries);
        for (int i = 0; i < switches.length; i++) {
            tag.putBoolean("switch_" + i, switches[i]);
        }
    }
}
