package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.oc.OCData;

import java.util.Map;

/**
 * Rack switch board, component "switch_board".
 * Persisted / client-synced keys (CUSTOM_DATA): {@code switch_N} (boolean), N = 0..3.
 */
public class EnvironmentRackSwitchBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final boolean[] switches = new boolean[4];

    public EnvironmentRackSwitchBoard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("switch_board", Visibility.Network)
                .withConnector()
                .create());
    }

    private int checkIndex(Arguments args) {
        int index = args.checkInteger(0);
        if (index < 1 || index > switches.length) throw new IllegalArgumentException("index out of range");
        return index - 1;
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Returns the state of the switch (1-4).")
    public Object[] getSwitch(Context context, Arguments args) {
        return new Object[]{switches[checkIndex(args)]};
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Alias for getSwitch.")
    public Object[] isActive(Context context, Arguments args) {
        return getSwitch(context, args);
    }

    @Callback(doc = "function(index:number, state:boolean):boolean -- Sets the state of the switch (1-4).")
    public Object[] setSwitch(Context context, Arguments args) {
        int index = checkIndex(args);
        boolean state = args.checkBoolean(1);
        if (switches[index] == state) return new Object[]{false};
        flip(index, state);
        return new Object[]{true};
    }

    @Callback(doc = "function(index:number, state:boolean):boolean -- Alias for setSwitch.")
    public Object[] setActive(Context context, Arguments args) {
        return setSwitch(context, args);
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of switches (4).")
    public Object[] getSwitchCount(Context context, Arguments args) {
        return new Object[]{switches.length};
    }

    private void flip(int index, boolean state) {
        switches[index] = state;
        markRackDirty();
        if (node() != null) {
            node().sendToReachable("computer.signal", "switch_flipped", index + 1, state);
        }
    }

    private void markRackDirty() {
        if (host instanceof Rack rack) {
            int index = rack.indexOfMountable(this);
            if (index >= 0) rack.markChanged(index);
        } else if (host != null) {
            host.markChanged();
        }
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
            int sw = Math.max(0, Math.min(3, (int) Math.floor(hitX * 4.0f)));
            flip(sw, !switches[sw]);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, switches[sw] ? 1.0f : 0.8f);
            player.displayClientMessage(Component.translatable(switches[sw]
                    ? "message.computronics.switch_board.on" : "message.computronics.switch_board.off", sw + 1), true);
        }
        return true;
    }

    private void writeState(CompoundTag tag) {
        for (int i = 0; i < switches.length; i++) tag.putBoolean("switch_" + i, switches[i]);
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        CompoundTag tag = OCData.read(holder);
        for (int i = 0; i < switches.length; i++) {
            if (tag.contains("switch_" + i)) switches[i] = tag.getBoolean("switch_" + i);
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, this::writeState);
    }

    @Override
    public void describeForClient(MutableDataComponentHolder holder) {
        OCData.write(holder, this::writeState);
    }
}
