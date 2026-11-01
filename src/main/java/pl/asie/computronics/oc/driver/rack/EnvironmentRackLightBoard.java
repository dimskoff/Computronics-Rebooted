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
import pl.asie.computronics.item.rack.ItemRackLightBoard;

import java.util.Map;

public class EnvironmentRackLightBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final int[] colors = new int[42];
    private final boolean[] active = new boolean[42];
    private int mode = 1;

    public EnvironmentRackLightBoard(EnvironmentHost host) {
        this(host, 1);
    }

    public EnvironmentRackLightBoard(EnvironmentHost host, int initialMode) {
        this.host = host;
        this.mode = Math.max(1, Math.min(5, initialMode));
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("light_board", Visibility.Network)
                .withConnector()
                .create());
    }

    private int maxLights() {
        return ItemRackLightBoard.getLightCount(mode);
    }

    private int resolveIndex(int input) {
        int max = maxLights();
        // Support 1-based (Computronics legacy: 1..N) and 0-based (0..N-1)
        if (input >= 1 && input <= max) {
            return input - 1;
        } else if (input >= 0 && input < max) {
            return input;
        }
        return -1;
    }

    @Callback(doc = "function(index:number, color:number):boolean -- Sets RGB color of an indicator light (1-N).")
    public Object[] setColor(Context context, Arguments args) {
        int idx = resolveIndex(args.checkInteger(0));
        if (idx < 0) return new Object[]{false, "index out of range"};
        int color = args.checkInteger(1) & 0xFFFFFF;
        colors[idx] = color;
        active[idx] = true;
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(doc = "function(index:number, color:number):boolean -- Sets RGB color of an indicator light (alias).")
    public Object[] setLightColor(Context context, Arguments args) {
        return setColor(context, args);
    }

    @Callback(direct = true, doc = "function(index:number):number -- Gets RGB color of an indicator light.")
    public Object[] getColor(Context context, Arguments args) {
        int idx = resolveIndex(args.checkInteger(0));
        if (idx < 0) return new Object[]{null, "index out of range"};
        return new Object[]{colors[idx]};
    }

    @Callback(direct = true, doc = "function(index:number):number -- Gets RGB color of an indicator light (alias).")
    public Object[] getLightColor(Context context, Arguments args) {
        return getColor(context, args);
    }

    @Callback(doc = "function(index:number, state:boolean):boolean -- Turns specified light on or off.")
    public Object[] setActive(Context context, Arguments args) {
        int idx = resolveIndex(args.checkInteger(0));
        if (idx < 0) return new Object[]{false, "index out of range"};
        active[idx] = args.checkBoolean(1);
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Returns true if light is active.")
    public Object[] isActive(Context context, Arguments args) {
        int idx = resolveIndex(args.checkInteger(0));
        if (idx < 0) return new Object[]{false, "index out of range"};
        return new Object[]{active[idx]};
    }

    @Callback(doc = "function(colors:table):boolean -- Sets colors of all lights from table.")
    public Object[] setColors(Context context, Arguments args) {
        Map<?, ?> map = args.checkTable(0);
        int max = maxLights();
        for (int i = 0; i < max; i++) {
            Object val = map.get(i + 1);
            if (val == null) val = map.get(i);
            if (val instanceof Number n) {
                colors[i] = n.intValue() & 0xFFFFFF;
                active[i] = true;
            }
        }
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Gets total number of lights for current mode.")
    public Object[] getLightCount(Context context, Arguments args) {
        return new Object[]{maxLights()};
    }

    @Callback(doc = "function():boolean -- Clears all light colors and states.")
    public Object[] clear(Context context, Arguments args) {
        int max = maxLights();
        for (int i = 0; i < max; i++) {
            colors[i] = 0;
            active[i] = false;
        }
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(doc = "function(mode:number):number -- Sets LED display mode (1: 4 LEDs, 2: 5 LEDs, 3: 10 LEDs, 4: 12 LEDs, 5: 42 LEDs).")
    public Object[] setMode(Context context, Arguments args) {
        this.mode = Math.max(1, Math.min(args.checkInteger(0), 5));
        markRackDirty();
        return new Object[]{mode};
    }

    @Callback(direct = true, doc = "function():number -- Gets current LED display mode.")
    public Object[] getMode(Context context, Arguments args) {
        return new Object[]{mode};
    }

    private void markRackDirty() {
        if (host instanceof Rack rack) {
            rack.markChanged(rack.indexOfMountable(this));
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
                DeviceInfo.DeviceAttribute.Description, "Rack Light Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "LED-Board-" + maxLights()
        );
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack stack, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            this.mode = (mode % 5) + 1;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    String.format("§b[Light Board] §fMode: §e%s", ItemRackLightBoard.getModeName(mode))), true);
            markRackDirty();
        }
        return true;
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(tag, registries);
        if (tag.contains("mode")) {
            mode = Math.max(1, Math.min(5, tag.getInt("mode")));
        }
        for (int i = 0; i < 42; i++) {
            if (tag.contains("c_" + i)) {
                colors[i] = tag.getInt("c_" + i);
            }
            if (tag.contains("a_" + i)) {
                active[i] = tag.getBoolean("a_" + i);
            }
        }
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveData(tag, registries);
        tag.putInt("mode", mode);
        for (int i = 0; i < 42; i++) {
            tag.putInt("c_" + i, colors[i]);
            tag.putBoolean("a_" + i, active[i]);
        }
    }
}
